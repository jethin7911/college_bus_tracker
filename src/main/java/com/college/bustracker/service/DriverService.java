package com.college.bustracker.service;

import com.college.bustracker.dto.*;
import com.college.bustracker.entity.Driver;
import com.college.bustracker.repository.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class DriverService {

    @Autowired
    private DriverRepository driverRepository;

    // Driver login (phone number only)
    public DriverLoginResponseDTO login(DriverLoginRequestDTO request) {

        Optional<Driver> driverOpt =
                driverRepository.findByPhoneNumber(request.getPhoneNumber());

        //  Not added by admin → reject
        if (driverOpt.isEmpty()) {
            DriverLoginResponseDTO response = new DriverLoginResponseDTO();
            response.setSuccess(false);
            response.setMessage("Unauthorized. Contact admin to add your number.");
            return response;
        }

        // Authorized driver
        Driver driver = driverOpt.get();

        DriverLoginResponseDTO response = new DriverLoginResponseDTO();
        response.setDriverId(driver.getId());
        //response.setPhoneNumber(driver.getPhoneNumber());
        //response.setName(driver.getName());
        response.setSuccess(true);
        response.setMessage("Login successful");

        return response;
    }

    // Get all drivers
    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(driver -> new DriverDTO(
                        driver.getId(),
                        driver.getPhoneNumber(),
                        driver.getName()
                ))
                .collect(Collectors.toList());
    }

    // Add new driver
    public ApiResponseDTO addDriver(DriverDTO driverDTO) {

        if (driverRepository.existsByPhoneNumber(driverDTO.getPhoneNumber())) {
            return new ApiResponseDTO(false, "Phone number already exists");
        }

        Driver driver = new Driver();
        driver.setPhoneNumber(driverDTO.getPhoneNumber());
        driver.setName(driverDTO.getName());

        Driver saved = driverRepository.save(driver);
        System.out.println("✅ Driver saved with ID: " + saved.getId()); // Debug log
        return new ApiResponseDTO(true, "Driver added successfully", saved.getId());
    }

    // Delete driver
    public ApiResponseDTO deleteDriver(Long driverId) {

        if (!driverRepository.existsById(driverId)) {
            return new ApiResponseDTO(false, "Driver not found");
        }

        driverRepository.deleteById(driverId);
        System.out.println("✅ Driver deleted with ID: " + driverId); // Debug log
        return new ApiResponseDTO(true, "Driver deleted successfully");
    }
}