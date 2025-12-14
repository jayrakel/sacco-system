package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.AuthRequest;
import com.sacco.sacco_system.dto.AuthResponse;
import com.sacco.sacco_system.dto.ChangePasswordRequest;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.entity.VerificationToken;
import com.sacco.sacco_system.repository.UserRepository;
import com.sacco.sacco_system.repository.VerificationTokenRepository;
import com.sacco.sacco_system.security.JwtService;
import com.sacco.sacco_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // Dependencies for Email Verification
    private final EmailService emailService;
    private final VerificationTokenRepository tokenRepository;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(User.Role.MEMBER);
        }

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // --- Enforce Email Verification ---
        // Allow ADMIN to bypass (because of fake email), everyone else MUST verify
        if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Access Denied: Please verify your email first.");
            // We use 403 Forbidden for unverified users so frontend can show "Resend" button
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }
        // ---------------------------------------

        var token = jwtService.generateToken(user);

        // Check System Setup Status (For Admin Only)
        boolean setupRequired = false;
        if (user.getRole() == User.Role.ADMIN) {
            if (!userRepository.existsByRole(User.Role.CHAIRPERSON)) {
                setupRequired = true;
            }
        }

        // ✅ UPDATED RESPONSE BUILDER
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())   // ✅ Populate First Name
                .lastName(user.getLastName())     // ✅ Populate Last Name
                .memberNumber(user.getMemberNumber()) // ✅ Populate Member No
                .role(user.getRole().toString())
                .mustChangePassword(user.isMustChangePassword())
                .systemSetupRequired(setupRequired)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Current password is incorrect");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        Map<String, Object> response = new HashMap<>();

        var userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.isEmailVerified()) {
                response.put("success", false);
                response.put("message", "This account is already verified. Please login.");
                return ResponseEntity.badRequest().body(response);
            }

            // 1. Delete any old token to prevent clutter
            tokenRepository.deleteByUser(user);

            // 2. Generate new token
            String newToken = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(user, newToken);
            tokenRepository.save(verificationToken);

            // 3. Send Email (Without password this time)
            emailService.resendVerificationToken(user.getEmail(), user.getFirstName(), newToken);
        }

        // Always return success to prevent email enumeration (security best practice)
        response.put("success", true);
        response.put("message", "If an account exists, a new verification link has been sent.");
        return ResponseEntity.ok(response);
    }
}