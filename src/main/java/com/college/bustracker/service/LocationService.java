package com.college.bustracker.service;

import com.college.bustracker.dto.BusLocationResponseDTO;
import com.college.bustracker.dto.LocationBroadcastDTO;
import com.college.bustracker.dto.LocationDTO;
import com.college.bustracker.entity.Assignment;
import com.college.bustracker.entity.Location;
import com.college.bustracker.repository.AssignmentRepository;
import com.college.bustracker.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Transactional
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    private final Map<Long, CurrentLocation> activeLocations = new ConcurrentHashMap<>();


    private volatile List<Location> pendingLocations = new CopyOnWriteArrayList<>();


    private final Map<Long, Assignment> assignmentCache = new ConcurrentHashMap<>();


    private static class CurrentLocation {
        Long busId;
        String busName;
        Long assignmentId;
        Double latitude;
        Double longitude;
        LocalDateTime timestamp;

        CurrentLocation(Long busId, String busName, Long assignmentId,
                        Double latitude, Double longitude, LocalDateTime timestamp) {
            this.busId = busId;
            this.busName = busName;
            this.assignmentId = assignmentId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp;
        }
    }

    // ─── WebSocket handler calls this. Updates RAM + buffer, does NOT hit DB ─
    public LocationBroadcastDTO updateLocationAndGetBroadcast(LocationDTO locationDTO) {

        // Try cache first, fall back to DB only on cache miss
        Assignment assignment = assignmentCache.get(locationDTO.getAssignmentId());
        if (assignment == null) {
            Optional<Assignment> opt = assignmentRepository.findById(locationDTO.getAssignmentId());
            if (opt.isEmpty() || !opt.get().getIsActive()) {
                return null;
            }
            assignment = opt.get();
            assignmentCache.put(assignment.getId(), assignment);
        }

        // If assignment was deactivated since we cached it, skip
        if (!assignment.getIsActive()) {
            assignmentCache.remove(assignment.getId());
            return null;
        }

        Long busId = assignment.getBus().getId();
        String busName = assignment.getBus().getBusName();
        LocalDateTime now = LocalDateTime.now();

        // 1. Update in-memory map (instant, students read from here)
        activeLocations.put(busId, new CurrentLocation(
                busId, busName, assignment.getId(),
                locationDTO.getLatitude(), locationDTO.getLongitude(), now
        ));

        // 2. Add to pending buffer (will be flushed to DB by the scheduled task)
        Location location = new Location();
        location.setAssignment(assignment);
        location.setLatitude(locationDTO.getLatitude());
        location.setLongitude(locationDTO.getLongitude());
        location.setTimestamp(now);
        pendingLocations.add(location);

        // 3. Return broadcast DTO (WebSocket pushes this to subscribed students)
        LocationBroadcastDTO broadcastDto = new LocationBroadcastDTO(
                busId, assignment.getId(), busName,
                locationDTO.getLatitude(), locationDTO.getLongitude(), now
        );

        // Broadcast to bus-specific topic (e.g., /topic/bus/123)
        messagingTemplate.convertAndSend("/topic/bus/" + busId, broadcastDto);

        // Also broadcast to general topic for backwards compatibility
        messagingTemplate.convertAndSend("/topic/bus-location", broadcastDto);

        System.out.println("Location broadcasted for bus " + busId + " to /topic/bus/" + busId);

        return broadcastDto;
    }

    // ─── Flush buffer to DB every 30 seconds ─────────────────────────────────
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void flushPendingLocations() {
        if (pendingLocations.isEmpty()) return;

        // Atomic swap: grab current list, replace with a fresh empty one
        List<Location> toSave = pendingLocations;
        pendingLocations = new CopyOnWriteArrayList<>();

        if (toSave.isEmpty()) return;

        try {
            locationRepository.saveAll(toSave); // single batch INSERT
            System.out.println("Flushed " + toSave.size() + " locations to database");
        } catch (Exception e) {
            System.err.println("Failed to flush locations: " + e.getMessage());
            // Put them back so they don't get lost
            pendingLocations.addAll(toSave);
        }
    }
    public BusLocationResponseDTO getCurrentLocation(Long busId) {
        CurrentLocation current = activeLocations.get(busId);

        if (current == null) {
            // Not in RAM (app restarted?) — fall back to DB
            Optional<Assignment> activeAssignment = assignmentRepository.findByBusIdAndIsActiveTrue(busId);
            if (activeAssignment.isPresent()) {
                Optional<Location> lastLocation =
                        locationRepository.findLatestByAssignmentId(activeAssignment.get().getId());
                if (lastLocation.isPresent()) {

                    Location loc = lastLocation.get();

                    return new BusLocationResponseDTO(
                            busId,
                            activeAssignment.get().getBus().getBusName(),
                            loc.getLatitude(),
                            loc.getLongitude(),
                            loc.getTimestamp(),
                            loc.getLatitude() != null && loc.getLongitude() != null
                    );
                }
            }
            return new BusLocationResponseDTO(busId, null, null, null, null, false);
        }

        boolean hasLocation =
                current.latitude != null &&
                        current.longitude != null;

        return new BusLocationResponseDTO(
                current.busId,
                current.busName,
                current.latitude,
                current.longitude,
                current.timestamp,
                hasLocation
        );
    }
    public List<BusLocationResponseDTO> getAllBusLocations() {
        List<Assignment> activeAssignments = assignmentRepository.findByIsActiveTrueOrderByStartedAtDesc();
        return activeAssignments.stream()
                .map(a -> getCurrentLocation(a.getBus().getId()))
                .collect(Collectors.toList());
    }

    // Called when driver stops tracking
    public void removeLocation(Long busId) {
        activeLocations.remove(busId);
    }
    //  Auto-timeout: if no GPS update for 10 min, stop the assignment
    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void checkStaleAssignments() {
        LocalDateTime timeout = LocalDateTime.now().minus(10, ChronoUnit.MINUTES);
        List<Assignment> staleAssignments = assignmentRepository.findStaleAssignments(timeout);

        for (Assignment assignment : staleAssignments) {
            CurrentLocation current = activeLocations.get(assignment.getBus().getId());

            if (current != null && current.timestamp.isBefore(timeout)) {
                assignment.setIsActive(false);
                assignment.setEndedAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
                activeLocations.remove(assignment.getBus().getId());
                assignmentCache.remove(assignment.getId());
                System.out.println("Auto-stopped stale assignment: " + assignment.getId());
            }
        }
    }
    // ─── Called by DataInitializer on boot to pre-populate active assignments ─
    public void seedAssignment(Assignment assignment) {
        activeLocations.putIfAbsent(
                assignment.getBus().getId(),
                new CurrentLocation(
                        assignment.getBus().getId(),
                        assignment.getBus().getBusName(),
                        assignment.getId(),
                        null, null, LocalDateTime.now()
                )
        );
        assignmentCache.putIfAbsent(assignment.getId(), assignment);
    }
    // ─── Cleanup: delete location records older than 24 hours ───────────────
    //     Keeps the DB table small, especially important on free tier
    @Scheduled(fixedRate = 3600000) // every 1 hour
    public void cleanupOldLocations() {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        try {
            locationRepository.deleteByTimestampBefore(cutoff);
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }
}