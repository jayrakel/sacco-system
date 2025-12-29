package com.sacco.sacco_system.modules.auth.service.auth.impl;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthVerificationHandler {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);

        if (verificationToken == null) {
            throw new RuntimeException("Invalid verification token");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        auditService.logSuccess(user, AuditLog.Actions.EMAIL_VERIFICATION, "User",
                user.getId().toString(), "Email verified successfully");
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        tokenRepository.deleteByUser(user);

        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenString)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), tokenString);

        auditService.logSuccess(user, "RESEND_VERIFICATION", "User",
                user.getId().toString(), "Verification email resent");
    }
}