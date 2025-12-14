package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.entity.VerificationToken;
import com.sacco.sacco_system.repository.UserRepository;
import com.sacco.sacco_system.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerifyController {

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        Map<String, Object> response = new HashMap<>();

        // 1. Find Token
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElse(null);

        if (verificationToken == null) {
            response.put("success", false);
            response.put("message", "Invalid token.");
            return ResponseEntity.badRequest().body(response);
        }

        // 2. Check Expiry
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.put("success", false);
            response.put("message", "Token has expired. Please request a new one.");
            return ResponseEntity.badRequest().body(response);
        }

        // 3. Verify User
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // 4. Delete Token (Prevent reuse)
        tokenRepository.delete(verificationToken);

        response.put("success", true);
        response.put("message", "Email verified successfully!");
        return ResponseEntity.ok(response);
    }
}