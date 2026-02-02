package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusSelectionDTO {
    private Long driverId;
    private Long busId;
    private Boolean forceStart;

    // Constructor for backward compatibility (when forceStart is not provided)
    public BusSelectionDTO(Long driverId, Long busId) {
        this.driverId = driverId;
        this.busId = busId;
        this.forceStart = false; // Default to false
    }
}