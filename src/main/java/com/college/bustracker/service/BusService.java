package com.college.bustracker.service;

import com.college.bustracker.dto.ApiResponseDTO;
import com.college.bustracker.dto.BusDTO;
import com.college.bustracker.entity.Bus;
import com.college.bustracker.repository.AssignmentRepository;
import com.college.bustracker.repository.BusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BusService {

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    // Get all buses
    public List<BusDTO> getAllBuses() {
        return busRepository.findAll().stream()
                .map(bus -> new BusDTO(bus.getId(), bus.getBusName()))
                .collect(Collectors.toList());
    }

    // Get available buses (not currently being tracked)
    public List<BusDTO> getAvailableBuses() {
        List<Bus> allBuses = busRepository.findAll();

        return allBuses.stream()
                .filter(bus -> assignmentRepository.findByBusIdAndIsActiveTrue(bus.getId()).isEmpty())
                .map(bus -> new BusDTO(bus.getId(), bus.getBusName()))
                .collect(Collectors.toList());
    }

    // Add new bus
    public ApiResponseDTO addBus(BusDTO busDTO) {
        if (busRepository.existsByBusName(busDTO.getBusName())) {
            return new ApiResponseDTO(false, "Bus name already exists");
        }

        Bus bus = new Bus();
        bus.setBusName(busDTO.getBusName());

        Bus saved = busRepository.save(bus);
        System.out.println("Bus saved with ID: " + saved.getId()); // Debug log
        return new ApiResponseDTO(true, "Bus added successfully", saved.getId());
    }

    // Delete bus
    public ApiResponseDTO deleteBus(Long busId) {
        if (!busRepository.existsById(busId)) {
            return new ApiResponseDTO(false, "Bus not found");
        }

        // Check if bus has active assignment
        if (assignmentRepository.findByBusIdAndIsActiveTrue(busId).isPresent()) {
            return new ApiResponseDTO(false, "Cannot delete bus with active tracking");
        }

        busRepository.deleteById(busId);
        System.out.println("Bus deleted with ID: " + busId); // Debug log
        return new ApiResponseDTO(true, "Bus deleted successfully");
    }
}