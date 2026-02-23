package com.college.bustracker.config;

import com.college.bustracker.entity.Admin;
import com.college.bustracker.entity.Bus;
import com.college.bustracker.repository.AdminRepository;
import com.college.bustracker.repository.BusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!adminRepository.existsByUsername("nitcadmin")) {
            Admin superAdmin = new Admin();
            superAdmin.setUsername("nitcadmin");
            superAdmin.setPassword(passwordEncoder.encode("nitc@123"));
            superAdmin.setName("Super Admin");
            superAdmin.setCreatedBy(null);

            adminRepository.save(superAdmin);
           // System.out.println("Super admin created: nitcadmin / nitc@123");
        }

        if (busRepository.count() == 0) {
            String[] busNames = {"Boys 1", "Boys 2", "Girls 1", "Girls 2", "Day Scholar 1", "Day Scholar 2"};

            for (String busName : busNames) {
                Bus bus = new Bus();
                bus.setBusName(busName);
                busRepository.save(bus);
            }

            System.out.println("✅ Created 6 default buses");
        }
    }
}