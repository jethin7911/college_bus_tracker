package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationBroadcastDTO {
    private Long busId;           // Added for student filtering
    private Long assignmentId;
    private String busName;       // Optional, for display
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
}