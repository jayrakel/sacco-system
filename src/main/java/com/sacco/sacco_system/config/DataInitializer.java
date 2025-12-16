package com.sacco.sacco_system.config;

import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.UserRepository;
import com.sacco.sacco_system.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountingService accountingService;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // 1. Initialize Admin
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .username(adminEmail) // ✅ ADDED: Required by User entity
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Admin")
                    .role(User.Role.ADMIN)
                    .phoneNumber("0700000000") // Added default phone to satisfy constraints if any
                    .memberNumber("ADMIN")     // Added default member number
                    .emailVerified(true)
                    .enabled(true)             // Explicitly enable
                    .createdAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            log.info("✅ Default Admin created: {}", adminEmail);
        }

        // 2. Initialize Finance System
        try {
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();
            log.info("✅ Chart of Accounts & Mappings checked/initialized.");
        } catch (Exception e) {
            log.error("⚠️ Warning: Could not initialize Chart of Accounts: {}", e.getMessage());
        }
    }
}