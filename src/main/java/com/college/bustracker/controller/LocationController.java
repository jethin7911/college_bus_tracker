package com.college.bustracker.controller;

import com.college.bustracker.dto.BusLocationResponseDTO;
import com.college.bustracker.dto.LocationDTO;
import com.college.bustracker.dto.LocationBroadcastDTO;
import com.college.bustracker.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
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
    @SendTo("/topic/location-updates")
    public LocationBroadcastDTO handleLocationUpdate(LocationDTO locationDTO) {
        // Update location and get the broadcast DTO with busId
        LocationBroadcastDTO broadcast = locationService.updateLocationAndGetBroadcast(locationDTO);
        return broadcast; // Broadcast to all subscribers WITH busId
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