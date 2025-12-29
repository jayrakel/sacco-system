package com.sacco.sacco_system.modules.auth.service.setup.impl;

import com.sacco.sacco_system.modules.auth.controller.setup.SetupController.CriticalAdminRequest;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupAdminWorker {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.official-email-domain:sacco.com}")
    private String officialEmailDomain;

    @Transactional
    public Map<String, Object> createCriticalAdmin(CriticalAdminRequest request) {
        try {
            // Validate role
            User.Role role;
            try {
                role = User.Role.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                return Map.of("success", false, "email", request.getEmail(), "error", "Invalid role: " + request.getRole());
            }

            // Check if admin account already exists
            String officialEmail = generateOfficialEmail(role);
            if (userRepository.findByOfficialEmail(officialEmail).isPresent()) {
                return Map.of("success", false, "email", request.getEmail(), "error", "Admin role already assigned to another user");
            }

            // Generate temporary password
            String tempPassword = generateTemporaryPassword();
            String encodedPassword = passwordEncoder.encode(tempPassword);

            // CREATE ADMIN ACCOUNT
            User adminUser = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(officialEmail) // Primary login is official email for admins
                    .phoneNumber(request.getPhoneNumber())
                    .role(role)
                    .officialEmail(officialEmail)
                    .password(encodedPassword)
                    .enabled(true)
                    .emailVerified(true)
                    .mustChangePassword(true)
                    .build();

            User savedAdmin = userRepository.save(adminUser);

            // Generate verification token (internal use or backup)
            String verificationToken = UUID.randomUUID().toString();
            tokenRepository.save(new VerificationToken(savedAdmin, verificationToken));

            // Send verification email to personal email provided in setup
            try {
                emailService.sendVerificationEmail(
                        request.getEmail(), // Send to their personal email
                        savedAdmin.getFirstName(),
                        tempPassword,
                        verificationToken
                );
            } catch (Exception e) {
                log.warn("Email sending failed for {}: {}", request.getEmail(), e.getMessage());
            }

            return Map.of(
                    "success", true,
                    "email", request.getEmail(),
                    "role", role.toString(),
                    "officialEmail", savedAdmin.getOfficialEmail(),
                    "tempPassword", tempPassword
            );

        } catch (Exception e) {
            log.error("Error creating account for {}: {}", request.getEmail(), e.getMessage());
            return Map.of("success", false, "email", request.getEmail(), "error", e.getMessage());
        }
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private String generateOfficialEmail(User.Role role) {
        return role.toString().toLowerCase().replace("_", "") + "@" + officialEmailDomain;
    }
}