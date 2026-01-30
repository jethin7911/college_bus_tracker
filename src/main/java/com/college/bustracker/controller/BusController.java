package com.college.bustracker.controller;

import com.college.bustracker.dto.BusDTO;
import com.college.bustracker.service.BusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@CrossOrigin(origins = "*")
public class BusController {

    @Autowired
    private BusService busService;

    // Get all buses (for student view)
    @GetMapping
    public ResponseEntity<List<BusDTO>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllBuses());
    }
}