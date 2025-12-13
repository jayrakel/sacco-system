package com.sacco.sacco_system.config;

import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        createDefaultAdminIfNotExist();
    }

    private void createDefaultAdminIfNotExist() {
        // Check if a user with the admin email exists
        Optional<User> adminExists = userRepository.findByEmail(adminEmail);

        if (adminExists.isEmpty()) {
            System.out.println("⚠️ No Admin account found. Creating default Admin...");

            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .username("admin") // Can be same as email or separate
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword)) // Hash the password!
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .build();

            userRepository.save(admin);
            System.out.println("✅ Default Admin created successfully!");
            System.out.println("👉 Email: " + adminEmail);
            System.out.println("👉 Password: " + adminPassword); // Only prints to console on dev
        } else {
            System.out.println("ℹ️ Admin account already exists.");
        }
    }
}