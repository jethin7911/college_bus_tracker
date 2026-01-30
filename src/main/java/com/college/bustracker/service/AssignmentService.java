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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private LocationService locationService;

    // Start tracking (driver selects bus)
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

        // Check if driver already has active assignment
        Optional<Assignment> driverActive = assignmentRepository.findByDriverIdAndIsActiveTrue(request.getDriverId());
        if (driverActive.isPresent()) {
            return new ApiResponseDTO(false, "You are already tracking another bus. Stop it first.");
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

        locationService.seedAssignment(saved);

        return new ApiResponseDTO(true, "Tracking started", saved.getId());
    }

    // Stop tracking
    public ApiResponseDTO stopTracking(Long assignmentId) {
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);

        if (assignmentOpt.isEmpty()) {
            return new ApiResponseDTO(false, "Assignment not found");
        }

        Assignment assignment = assignmentOpt.get();
        assignment.setIsActive(false);
        assignment.setEndedAt(LocalDateTime.now());

        assignmentRepository.save(assignment);

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