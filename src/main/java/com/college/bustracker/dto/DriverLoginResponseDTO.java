package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverLoginResponseDTO {
    private Long driverId;
    private String phoneNumber;
    private String name;
    private boolean authorized;
    private String message;

    public void setSuccess(boolean b) {
    }
}