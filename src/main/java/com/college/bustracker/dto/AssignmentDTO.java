package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDTO {
    private Long assignmentId;
    private Long busId;
    private String busName;
    private Long driverId;
    private String driverPhone;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isActive;
}