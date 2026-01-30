package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusLocationResponseDTO {
    private Long busId;
    private String busName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private boolean hasLocation;
}