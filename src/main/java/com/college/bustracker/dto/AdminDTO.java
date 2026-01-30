package com.college.bustracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDTO {
    private Long id;
    private String username;
    private String name;
    private Long createdBy;
}