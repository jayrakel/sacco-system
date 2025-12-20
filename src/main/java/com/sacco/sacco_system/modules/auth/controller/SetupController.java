package com.sacco.sacco_system.modules.auth.controller;

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Data
    public static class CriticalAdminRequest {
        private String role;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
    }

    @Data
    public static class CriticalAdminsPayload {
        private List<CriticalAdminRequest> admins;
    }

    /**
     * Create critical admin users (Chairperson, Secretary, Treasurer, Loan Officer, etc.)
     * Generates temporary passwords and sends them via email
     */
    @PostMapping("/critical-admins")
    @Transactional
    public ResponseEntity<Map<String, Object>> createCriticalAdmins(@RequestBody CriticalAdminsPayload payload) {
        try {
            if (payload.getAdmins() == null || payload.getAdmins().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No admins provided"
                ));
            }

            List<Map<String, Object>> createdAdmins = new ArrayList<>();
            
            for (CriticalAdminRequest request : payload.getAdmins()) {
                try {
                    Map<String, Object> result = createCriticalAdmin(request);
                    if ((Boolean) result.get("success")) {
                        createdAdmins.add(result);
                    } else {
                        createdAdmins.add(result);
                    }
                } catch (Exception e) {
                    log.error("Failed to create admin {}: {}", request.getEmail(), e.getMessage(), e);
                    createdAdmins.add(Map.of(
                            "success", false,
                            "email", request.getEmail(),
                            "error", e.getMessage()
                    ));
                }
            }

            long successCount = createdAdmins.stream().filter(a -> (Boolean) a.get("success")).count();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", successCount + " critical admin(s) created successfully. Check email for credentials.",
                    "count", successCount,
                    "total", createdAdmins.size(),
                    "admins", createdAdmins
            ));
        } catch (Exception e) {
            log.error("Failed to create critical admins: ", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to create critical admins: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * Create individual critical admin user with temporary password and email verification
     */
    public Map<String, Object> createCriticalAdmin(CriticalAdminRequest request) {
        try {
            log.info("Starting creation of admin: {}", request.getEmail());
            
            // Validate role
            User.Role role;
            try {
                role = User.Role.valueOf(request.getRole());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + request.getRole());
            }

            // Check if user already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("User with email " + request.getEmail() + " already exists");
            }

            // Generate temporary password
            String tempPassword = generateTemporaryPassword();
            log.info("Generated temporary password for: {}", request.getEmail());

            // Create user
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .role(role)
                    .officialEmail(generateOfficialEmail(role))
                    .password(passwordEncoder.encode(tempPassword))
                    .enabled(true)
                    .emailVerified(false)
                    .mustChangePassword(true)
                    .build();

            log.info("User object created, attempting to save to database: {}", request.getEmail());
            
            // Save user to database
            User savedUser = userRepository.save(user);
            log.info("✅ User saved to database with ID: {} - {}", savedUser.getId(), request.getEmail());

            // Generate and save email verification token
            String verificationToken = UUID.randomUUID().toString();
            VerificationToken vToken = new VerificationToken(savedUser, verificationToken);
            tokenRepository.save(vToken);
            log.info("✅ Generated and saved verification token for: {}", request.getEmail());

            // Send verification email with temporary password (asynchronously)
            try {
                emailService.sendVerificationEmail(
                        savedUser.getEmail(),
                        savedUser.getFirstName(),
                        tempPassword,
                        verificationToken
                );
                log.info("✅ Sent verification email to: {}", request.getEmail());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send email to {}, but user was created successfully: {}", request.getEmail(), e.getMessage());
                // Don't fail the whole operation if email fails
            }

            return Map.of(
                    "success", true,
                    "role", role.toString(),
                    "email", request.getEmail(),
                    "firstName", request.getFirstName(),
                    "lastName", request.getLastName(),
                    "message", "Account created and saved to database. Verification email sent with temporary credentials."
            );
        } catch (Exception e) {
            log.error("❌ Failed to create admin {}: {}", request.getEmail(), e.getMessage(), e);
            return Map.of(
                    "success", false,
                    "email", request.getEmail(),
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Generate a secure temporary password
     */
    private String generateTemporaryPassword() {
        // Generate a 12-character temporary password with mix of uppercase, lowercase, numbers, and special char
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*";

        String all = upperCase + lowerCase + numbers + specialChars;
        StringBuilder password = new StringBuilder();
        
        // Ensure at least one of each type
        password.append(upperCase.charAt((int) (Math.random() * upperCase.length())));
        password.append(lowerCase.charAt((int) (Math.random() * lowerCase.length())));
        password.append(numbers.charAt((int) (Math.random() * numbers.length())));
        password.append(specialChars.charAt((int) (Math.random() * specialChars.length())));

        // Fill rest randomly
        for (int i = 4; i < 12; i++) {
            password.append(all.charAt((int) (Math.random() * all.length())));
        }

        // Shuffle the password
        String tempPass = password.toString();
        char[] chars = tempPass.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    /**
     * Generate official email based on role
     * The same email is used for all people in that position - only password changes when officials rotate
     */
    private String generateOfficialEmail(User.Role role) {
        String rolePrefix = role.toString().toLowerCase().replace("_", "");
        return rolePrefix + "@sacco.local";
    }
}
