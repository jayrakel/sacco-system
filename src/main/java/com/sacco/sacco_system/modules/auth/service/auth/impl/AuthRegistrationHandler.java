package com.sacco.sacco_system.modules.auth.service.auth.impl;

import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.auth.service.jwt.JwtService;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthRegistrationHandler {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Transactional
    public Map<String, Object> register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) user.setRole(User.Role.MEMBER);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail());

        // Generate Verification Token
        String verificationToken = UUID.randomUUID().toString();
        VerificationToken vToken = VerificationToken.builder()
                .token(verificationToken)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(vToken);

        // Send Email
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        return Map.of(
                "success", true,
                "message", "User registered successfully. Please check email for verification.",
                "token", token
        );
    }
}