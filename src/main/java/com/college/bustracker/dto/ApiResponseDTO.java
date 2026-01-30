package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO {
    private boolean success;
    private String message;
    private Long assignmentId;

    public ApiResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}