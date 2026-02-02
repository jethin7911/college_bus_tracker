package com.college.bustracker.service;

import com.college.bustracker.dto.BusLocationResponseDTO;
import com.college.bustracker.dto.LocationBroadcastDTO;
import com.college.bustracker.dto.LocationDTO;
import com.college.bustracker.entity.Assignment;
import com.college.bustracker.entity.Location;
import com.college.bustracker.repository.AssignmentRepository;
import com.college.bustracker.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    // In-memory storage: busId -> current location
    private final Map<Long, CurrentLocation> activeLocations = new ConcurrentHashMap<>();

    // Inner class to store current location
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


    // Update location (called from WebSocket)
    public void updateLocation(LocationDTO locationDTO) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(locationDTO.getAssignmentId());

        if (assignmentOpt.isEmpty() || !assignmentOpt.get().getIsActive()) {
            return; // Invalid or inactive assignment
        }

        Assignment assignment = assignmentOpt.get();
        Long busId = assignment.getBus().getId();

        // Update in-memory (RAM) - FAST
        activeLocations.put(busId, new CurrentLocation(
                busId,
                assignment.getBus().getBusName(),
                assignment.getId(),
                locationDTO.getLatitude(),
                locationDTO.getLongitude(),
                LocalDateTime.now()
        ));

        // Optionally save to DB (can be done async or scheduled)
        // For now, we'll save every update
        Location location = new Location();
        location.setAssignment(assignment);
        location.setLatitude(locationDTO.getLatitude());
        location.setLongitude(locationDTO.getLongitude());
        location.setTimestamp(LocalDateTime.now());

        locationRepository.save(location);
    }

    // Get current location for a bus (for students)
    public BusLocationResponseDTO getCurrentLocation(Long busId) {
        CurrentLocation current = activeLocations.get(busId);

        if (current == null) {
            // Not in RAM, try to load from DB
            Optional<Assignment> activeAssignment = assignmentRepository.findByBusIdAndIsActiveTrue(busId);

            if (activeAssignment.isPresent()) {
                Optional<Location> lastLocation = locationRepository.findLatestByAssignmentId(activeAssignment.get().getId());

                if (lastLocation.isPresent()) {
                    Location loc = lastLocation.get();
                    return new BusLocationResponseDTO(
                            busId,
                            activeAssignment.get().getBus().getBusName(),
                            loc.getLatitude(),
                            loc.getLongitude(),
                            loc.getTimestamp(),
                            true
                    );
                }
            }

            // No location available
            return new BusLocationResponseDTO(busId, null, null, null, null, false);
        }

        // Return from RAM
        return new BusLocationResponseDTO(
                current.busId,
                current.busName,
                current.latitude,
                current.longitude,
                current.timestamp,
                true
        );

    }

    // Get all buses with their current locations
    public List<BusLocationResponseDTO> getAllBusLocations() {
        List<Assignment> activeAssignments = assignmentRepository.findByIsActiveTrueOrderByStartedAtDesc();

        return activeAssignments.stream()
                .map(assignment -> getCurrentLocation(assignment.getBus().getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    // Remove location when tracking stops
    public void removeLocation(Long busId) {
        activeLocations.remove(busId);
    }

    // Scheduled task: Auto-timeout stale assignments (every 5 minutes)
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkStaleAssignments() {
        LocalDateTime timeout = LocalDateTime.now().minus(10, ChronoUnit.MINUTES);

        List<Assignment> staleAssignments = assignmentRepository.findStaleAssignments(timeout);

        for (Assignment assignment : staleAssignments) {
            // Check if no location update in last 10 minutes
            CurrentLocation current = activeLocations.get(assignment.getBus().getId());

            if (current != null && current.timestamp.isBefore(timeout)) {
                // Mark as inactive
                assignment.setIsActive(false);
                assignment.setEndedAt(LocalDateTime.now());
                assignmentRepository.save(assignment);

                // Remove from RAM
                activeLocations.remove(assignment.getBus().getId());

                System.out.println("Auto-stopped stale assignment: " + assignment.getId());
            }
        }
    }

    public void seedAssignment(Assignment assignment) {
        activeLocations.putIfAbsent(
                assignment.getBus().getId(),
                new CurrentLocation(
                        assignment.getBus().getId(),
                        assignment.getBus().getBusName(),
                        assignment.getId(),
                        null,
                        null,
                        LocalDateTime.now()
                )
        );
    }

    // Add this method to your LocationService class
    public LocationBroadcastDTO updateLocationAndGetBroadcast(LocationDTO locationDTO) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(locationDTO.getAssignmentId());

        if (assignmentOpt.isEmpty() || !assignmentOpt.get().getIsActive()) {
            return null; // Invalid or inactive assignment
        }

        Assignment assignment = assignmentOpt.get();
        Long busId = assignment.getBus().getId();
        String busName = assignment.getBus().getBusName();

        // Update in-memory (RAM) - FAST
        activeLocations.put(busId, new CurrentLocation(
                busId,
                busName,
                assignment.getId(),
                locationDTO.getLatitude(),
                locationDTO.getLongitude(),
                LocalDateTime.now()
        ));

        // Save to DB (async or scheduled can be done here)
        Location location = new Location();
        location.setAssignment(assignment);
        location.setLatitude(locationDTO.getLatitude());
        location.setLongitude(locationDTO.getLongitude());
        location.setTimestamp(LocalDateTime.now());
        locationRepository.save(location);

        // Return broadcast DTO with busId included
        return new LocationBroadcastDTO(
                busId,
                assignment.getId(),
                busName,
                locationDTO.getLatitude(),
                locationDTO.getLongitude(),
                LocalDateTime.now()
        );
    }
}