package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Long assignmentId;
    private Double latitude;
    private Double longitude;
    //private LocalDateTime timestamp;
}