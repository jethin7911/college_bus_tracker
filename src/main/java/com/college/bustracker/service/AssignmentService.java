package com.college.bustracker.service;

import com.college.bustracker.dto.*;
import com.college.bustracker.entity.Assignment;
import com.college.bustracker.entity.Bus;
import com.college.bustracker.entity.Driver;
import com.college.bustracker.repository.AssignmentRepository;
import com.college.bustracker.repository.BusRepository;
import com.college.bustracker.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private LocationService locationService;

    // FIX: Start tracking with force-stop capability
    public ApiResponseDTO startTracking(BusSelectionDTO request) {

        // Check if driver exists
        Optional<Driver> driverOpt = driverRepository.findById(request.getDriverId());
        if (driverOpt.isEmpty()) {
            return new ApiResponseDTO(false, "Driver not found");
        }

        // Check if bus exists
        Optional<Bus> busOpt = busRepository.findById(request.getBusId());
        if (busOpt.isEmpty()) {
            return new ApiResponseDTO(false, "Bus not found");
        }

        // FIX: Check if driver already has active assignment
        Optional<Assignment> driverActive = assignmentRepository.findByDriverIdAndIsActiveTrue(request.getDriverId());
        if (driverActive.isPresent()) {
            // FIX: If forceStart is enabled, auto-stop the previous assignment
            if (request.getForceStart() != null && request.getForceStart()) {
                Assignment previousAssignment = driverActive.get();
                previousAssignment.setIsActive(false);
                previousAssignment.setEndedAt(LocalDateTime.now());
                assignmentRepository.save(previousAssignment);

                // FIX: Clear location from RAM
                locationService.removeLocation(previousAssignment.getBus().getId());

                System.out.println("Force-stopped previous assignment: " + previousAssignment.getId());
            } else {
                return new ApiResponseDTO(false, "You are already tracking another bus. Stop it first.");
            }
        }

        // Check if bus is already being tracked
        Optional<Assignment> busActive = assignmentRepository.findByBusIdAndIsActiveTrue(request.getBusId());
        if (busActive.isPresent()) {
            return new ApiResponseDTO(false, "This bus is already being tracked by another driver.");
        }

        // Create new assignment
        Assignment assignment = new Assignment();
        assignment.setBus(busOpt.get());
        assignment.setDriver(driverOpt.get());
        assignment.setStartedAt(LocalDateTime.now());
        assignment.setIsActive(true);

        Assignment saved = assignmentRepository.save(assignment);
        System.out.println("Assignment saved with ID: " + saved.getId()); // Debug log

        locationService.seedAssignment(saved);

        return new ApiResponseDTO(true, "Tracking started", saved.getId());
    }

    // FIX: Stop tracking with location cleanup
    public ApiResponseDTO stopTracking(Long assignmentId) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);

        if (assignmentOpt.isEmpty()) {
            return new ApiResponseDTO(false, "Assignment not found");
        }

        Assignment assignment = assignmentOpt.get();

        //  FIX: Clear location from RAM BEFORE updating database
        locationService.removeLocation(assignment.getBus().getId());

        assignment.setIsActive(false);
        assignment.setEndedAt(LocalDateTime.now());

        assignmentRepository.save(assignment);
        System.out.println("Assignment stopped with ID: " + assignmentId); // Debug log

        return new ApiResponseDTO(true, "Tracking stopped");
    }

    // Get all active assignments
    public List<AssignmentDTO> getActiveAssignments() {
        return assignmentRepository.findByIsActiveTrueOrderByStartedAtDesc().stream()
                .map(assignment -> new AssignmentDTO(
                        assignment.getId(),
                        assignment.getBus().getId(),
                        assignment.getBus().getBusName(),
                        assignment.getDriver().getId(),
                        assignment.getDriver().getPhoneNumber(),
                        assignment.getStartedAt(),
                        assignment.getEndedAt(),
                        assignment.getIsActive()
                ))
                .collect(Collectors.toList());
    }

    // Get assignment by ID
    public Optional<Assignment> getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }
}