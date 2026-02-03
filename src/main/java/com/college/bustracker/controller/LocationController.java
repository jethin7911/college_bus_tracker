package com.college.bustracker.controller;

import com.college.bustracker.dto.BusLocationResponseDTO;
import com.college.bustracker.dto.LocationDTO;
import com.college.bustracker.dto.LocationBroadcastDTO;
import com.college.bustracker.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class LocationController {

    @Autowired
    private LocationService locationService;

    // WebSocket endpoint - Driver sends location
    @MessageMapping("/location")
    @SendTo("/topic/bus-location")
    public LocationBroadcastDTO handleLocationUpdate(
            @Payload LocationDTO locationDTO
    ) {
        return locationService.updateLocationAndGetBroadcast(locationDTO);
    }

    // REST endpoint - Students get current bus location
    @GetMapping("/api/bus/{busId}/location")
    @ResponseBody
    public ResponseEntity<BusLocationResponseDTO> getCurrentLocation(@PathVariable Long busId) {
        BusLocationResponseDTO response = locationService.getCurrentLocation(busId);
        return ResponseEntity.ok(response);
    }

    // Get all active bus locations
    @GetMapping("/api/buses/locations")
    @ResponseBody
    public ResponseEntity<List<BusLocationResponseDTO>> getAllBusLocations() {
        return ResponseEntity.ok(locationService.getAllBusLocations());
    }
}