package com.sacco.sacco_system.modules.auth.controller;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SystemSettingRepository systemSettingRepository;
    private final AccountingService accountingService;

    @Value("${app.official-email-domain}")
    private String officialEmailDomain;

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
     * This iterates through the list and creates them safely.
     */
    @PostMapping("/critical-admins")
    // ❌ Removed @Transactional from here. 
    // We want to save each user individually. If one fails, the others should still exist.
    public ResponseEntity<?> createCriticalAdmins(@RequestBody CriticalAdminsPayload payload) {
        
        // Validate for duplicate emails within the payload
        Set<String> emailSet = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (CriticalAdminRequest admin : payload.getAdmins()) {
            if (!emailSet.add(admin.getEmail())) {
                duplicates.add(admin.getEmail() + " (" + admin.getRole() + ")");
            }
        }

        if (!duplicates.isEmpty()) {
            log.error("❌ Duplicate emails found in payload: {}", duplicates);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Duplicate emails found: " + String.join(", ", duplicates),
                "duplicates", duplicates
            ));
        }
        
        List<Map<String, Object>> results = new ArrayList<>();

        // Loop through every admin in the request and create them
        for (CriticalAdminRequest adminRequest : payload.getAdmins()) {
            Map<String, Object> result = createCriticalAdmin(adminRequest);
            results.add(result);
        }

        // 3. Initialize Chart of Accounts from accounts.json
        try {
            log.info("📊 Initializing Chart of Accounts...");
            accountingService.initChartOfAccounts();
            log.info("✅ Chart of Accounts initialized successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize Chart of Accounts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to initialize Chart of Accounts: " + e.getMessage(),
                "results", results
            ));
        }

        // 4. Mark Setup as Complete (So you don't get redirected back here)
        try {
            SystemSetting setupComplete = SystemSetting.builder()
                    .key("SETUP_COMPLETE")
                    .value("true")
                    .description("Flag to indicate system setup is done")
                    .dataType("BOOLEAN")
                    .build();
            systemSettingRepository.save(setupComplete);
            log.info("✅ System setup marked as complete");
        } catch (Exception e) {
            log.warn("⚠️ Setup flag might already exist or failed to save: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "success", true, 
            "message", "Setup process finished. Chart of Accounts initialized.", 
            "results", results
        ));
    }

    /**
     * Create individual critical admin user with temporary password and email verification
     * Uses its own transaction so one failure doesn't rollback everyone.
     */
    @Transactional 
    public Map<String, Object> createCriticalAdmin(CriticalAdminRequest request) {
        try {
            log.info("Starting creation of admin: {} with role: {}", request.getEmail(), request.getRole());
            
            // Validate role
            User.Role role;
            try {
                role = User.Role.valueOf(request.getRole());
                log.info("✅ Role validated: {}", role);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid role received: {}. Valid roles are: {}", request.getRole(), java.util.Arrays.toString(User.Role.values()));
                return Map.of("success", false, "email", request.getEmail(), "error", "Invalid role: " + request.getRole());
            }

            // Check if admin account already exists
            if (userRepository.findByOfficialEmail(generateOfficialEmail(role)).isPresent()) {
                return Map.of("success", false, "email", request.getEmail(), "error", "Admin role already assigned to another user");
            }

            // Generate temporary password
            String tempPassword = generateTemporaryPassword();
            String encodedPassword = passwordEncoder.encode(tempPassword);

            // CREATE ADMIN ACCOUNT (with official email)
            User adminUser = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(generateOfficialEmail(role)) // Admin uses official email
                    .phoneNumber(request.getPhoneNumber())
                    .role(role)
                    .officialEmail(generateOfficialEmail(role))
                    .passwordHash(encodedPassword)
                    .active(true)
                    .emailVerified(true)
                    .mustChangePassword(true)
                    .build();

            User savedAdmin = userRepository.save(adminUser);
            log.info("✅ Admin account created: {}", savedAdmin.getOfficialEmail());

            // Generate verification token
            String verificationToken = UUID.randomUUID().toString();
            tokenRepository.save(new VerificationToken(savedAdmin, verificationToken));

            // Send verification email to personal email
            try {
                log.info("Sending verification email to: {}", request.getEmail());
                emailService.sendVerificationEmail(
                        request.getEmail(),
                        savedAdmin.getFirstName(),
                        tempPassword,
                        verificationToken
                );
                log.info("📧 Verification email sent successfully to: {}", request.getEmail());
            } catch (Exception e) {
                log.warn("⚠️ Email failed for {}, but account is SAVED.", request.getEmail());
            }

            // 📝 CONSOLE FALLBACK - Log credentials for manual access
            log.warn("╔════════════════════════════════════════════════════════════════╗");
            log.warn("║ ADMIN ACCOUNT CREATED - SAVE THESE CREDENTIALS                ║");
            log.warn("╠════════════════════════════════════════════════════════════════╣");
            log.warn("║ Name:            {}", String.format("%-43s", savedAdmin.getFirstName() + " " + savedAdmin.getLastName()) + "║");
            log.warn("║ Role:            {}", String.format("%-43s", role.toString()) + "║");
            log.warn("║ Login Email:     {}", String.format("%-43s", savedAdmin.getOfficialEmail()) + "║");
            log.warn("║ Password:        {}", String.format("%-43s", tempPassword) + "║");
            log.warn("║ Phone:           {}", String.format("%-43s", savedAdmin.getPhoneNumber()) + "║");
            log.warn("╚════════════════════════════════════════════════════════════════╝");

            return Map.of(
                    "success", true,
                    "email", request.getEmail(),
                    "role", role.toString(),
                    "officialEmail", savedAdmin.getOfficialEmail(),
                    "tempPassword", tempPassword
            );

        } catch (Exception e) {
            log.error("❌ Critical error creating accounts for {}: {}", request.getEmail(), e.getMessage());
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