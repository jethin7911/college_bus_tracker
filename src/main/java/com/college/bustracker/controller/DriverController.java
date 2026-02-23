package com.college.bustracker.controller;

import com.college.bustracker.dto.*;
import com.college.bustracker.service.AssignmentService;
import com.college.bustracker.service.BusService;
import com.college.bustracker.service.DriverService;
import com.college.bustracker.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/driver")
@CrossOrigin(origins = "*")

public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private BusService busService;

    @Autowired
    private AssignmentService assignmentService;

    // Driver login
    @PostMapping("/login")
    public ResponseEntity<DriverLoginResponseDTO> login(@RequestBody DriverLoginRequestDTO request) {
        DriverLoginResponseDTO response = driverService.login(request);
        return ResponseEntity.ok(response);
    }

    // Get available buses (not currently being tracked)
    @GetMapping("/buses/available")
    public ResponseEntity<List<BusDTO>> getAvailableBuses() {
        return ResponseEntity.ok(busService.getAvailableBuses());
    }

    // Start tracking (driver selects bus)
    @PostMapping("/start-tracking")
    public ResponseEntity<ApiResponseDTO> startTracking(@RequestBody BusSelectionDTO request) {
        return ResponseEntity.ok(assignmentService.startTracking(request));
    }

    // Stop tracking
    @PostMapping("/stop-tracking")
    public ResponseEntity<ApiResponseDTO> stopTracking(@RequestParam Long assignmentId) {
        return ResponseEntity.ok(assignmentService.stopTracking(assignmentId));
    }

}