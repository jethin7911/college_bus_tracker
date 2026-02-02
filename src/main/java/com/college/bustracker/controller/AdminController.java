package com.college.bustracker.controller;

import com.college.bustracker.dto.*;
import com.college.bustracker.service.AdminService;
import com.college.bustracker.service.BusService;
import com.college.bustracker.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private BusService busService;

    @Autowired
    private DriverService driverService;

    // Admin login
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponseDTO> login(@RequestBody AdminLoginRequestDTO request) {
        AdminLoginResponseDTO response = adminService.login(request);

        if (!"Login successful".equals(response.getMessage())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    // Get all admins
    @GetMapping("/admins")
    public ResponseEntity<List<AdminDTO>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    // Add new admin
    @PostMapping("/admins")
    public ResponseEntity<ApiResponseDTO> addAdmin(
            @RequestBody AdminDTO adminDTO,
            @RequestParam Long createdBy) {
        return ResponseEntity.ok(adminService.addAdmin(adminDTO, createdBy));
    }

    // Delete admin
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<ApiResponseDTO> deleteAdmin(
            @PathVariable Long id,
            @RequestParam Long currentAdminId) {
        return ResponseEntity.ok(adminService.deleteAdmin(id, currentAdminId));
    }

    // Get all buses
    @GetMapping("/buses")
    public ResponseEntity<List<BusDTO>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }

    // Add new bus
    @PostMapping("/buses")
    public ResponseEntity<ApiResponseDTO> addBus(@RequestBody BusDTO busDTO) {
        return ResponseEntity.ok(busService.addBus(busDTO));
    }

    // Delete bus
    @DeleteMapping("/buses/{id}")
    public ResponseEntity<ApiResponseDTO> deleteBus(@PathVariable Long id) {
        return ResponseEntity.ok(busService.deleteBus(id));
    }

    // Get all drivers
    @GetMapping("/drivers")
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    // Add new driver
    @PostMapping("/drivers")
    public ResponseEntity<ApiResponseDTO> addDriver(@RequestBody DriverDTO driverDTO) {
        return ResponseEntity.ok(driverService.addDriver(driverDTO));
    }

    // Delete driver
    @DeleteMapping("/drivers/{id}")
    public ResponseEntity<ApiResponseDTO> deleteDriver(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.deleteDriver(id));
    }
}