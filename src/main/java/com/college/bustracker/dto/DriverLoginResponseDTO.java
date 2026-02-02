package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLoginResponseDTO {

    private Long driverId;
    private boolean success;
    private String message;

    // getters & setters

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
