package com.sacco.sacco_system.modules.system.controller;

import com.sacco.sacco_system.modules.system.dto.SetupRequest;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notifications.service.EmailService; // Import this
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SystemSetupController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // Inject this

    @PostMapping("/critical-admins")
    public ResponseEntity<Map<String, Object>> setupCriticalAdmins(@RequestBody SetupRequest request) {

        for (SetupRequest.NewAdmin adminDTO : request.getAdmins()) {

            if (userRepository.existsByEmail(adminDTO.getEmail())) {
                continue;
            }

            String tempPassword = UUID.randomUUID().toString().substring(0, 8);

            User user = User.builder()
                    .firstName(adminDTO.getFirstName())
                    .lastName(adminDTO.getLastName())
                    .email(adminDTO.getEmail())
                    .username(adminDTO.getEmail())
                    .phoneNumber(adminDTO.getPhoneNumber())
                    .role(adminDTO.getRole())
                    .password(passwordEncoder.encode(tempPassword))
                    .mustChangePassword(true)
                    .emailVerified(false) // Strict verification required
                    .enabled(true)
                    .build();

            User savedUser = userRepository.save(user);

            // Generate Token
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(savedUser, token);
            tokenRepository.save(verificationToken);

            // SEND REAL EMAIL
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    tempPassword,
                    token
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Admins created. Verification emails have been sent.");
        return ResponseEntity.ok(response);
    }
}