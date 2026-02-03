package com.college.bustracker.service;

import com.college.bustracker.dto.*;
import com.college.bustracker.entity.Admin;
import com.college.bustracker.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Admin Login
    public AdminLoginResponseDTO login(AdminLoginRequestDTO request) {
        Optional<Admin> adminOpt = adminRepository.findByUsername(request.getUsername());

        if (adminOpt.isEmpty()) {
            return new AdminLoginResponseDTO(null, null, null, "Invalid username");
        }

        Admin admin = adminOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            return new AdminLoginResponseDTO(null, null, null, "Invalid password");
        }

        return new AdminLoginResponseDTO(
                admin.getId(),
                admin.getUsername(),
                admin.getName(),
                "Login successful"
        );
    }

    // Get all admins
    public List<AdminDTO> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(admin -> new AdminDTO(
                        admin.getId(),
                        admin.getUsername(),
                        admin.getName(),
                        admin.getCreatedBy() != null ? admin.getCreatedBy().getId() : null
                ))
                .collect(Collectors.toList());
    }

    // Add new admin
    public ApiResponseDTO addAdmin(AdminDTO adminDTO, Long createdById) {
        if (adminRepository.existsByUsername(adminDTO.getUsername())) {
            return new ApiResponseDTO(false, "Username already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(adminDTO.getUsername());
        admin.setPassword(passwordEncoder.encode("defaultPassword123")); // Default password
        admin.setName(adminDTO.getName());

        if (createdById != null) {
            adminRepository.findById(createdById).ifPresent(admin::setCreatedBy);
        }

        Admin saved = adminRepository.save(admin);
        System.out.println("✅ Admin saved with ID: " + saved.getId()); // Debug log
        return new ApiResponseDTO(true, "Admin added successfully", saved.getId());
    }

    // Delete admin
    public ApiResponseDTO deleteAdmin(Long adminId, Long currentAdminId) {
        if (adminId.equals(currentAdminId)) {
            return new ApiResponseDTO(false, "Cannot delete yourself");
        }

        if (!adminRepository.existsById(adminId)) {
            return new ApiResponseDTO(false, "Admin not found");
        }

        adminRepository.deleteById(adminId);
        System.out.println("✅ Admin deleted with ID: " + adminId); // Debug log
        return new ApiResponseDTO(true, "Admin deleted successfully");
    }
}