package com.sacco.sacco_system.modules.auth.service.auth.impl;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.auth.dto.ChangePasswordRequest;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPasswordHandler {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            auditService.logFailure(currentUser, AuditLog.Actions.PASSWORD_CHANGE, "User",
                    currentUser.getId().toString(), "Password change failed", "Incorrect current password");
            throw new RuntimeException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);

        auditService.logSuccess(currentUser, AuditLog.Actions.PASSWORD_CHANGE, "User",
                currentUser.getId().toString(), "Password changed successfully");
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailOrOfficialEmail(email)
                .orElseThrow(() -> new RuntimeException("If that email exists, a reset link has been sent."));

        // 1. Manually check and remove existing token to prevent duplicates
        VerificationToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            tokenRepository.delete(existingToken);
            tokenRepository.flush();
        }

        // 2. Create new token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

        // 3. Send Email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
            log.info("Reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send reset email: {}", e.getMessage());
        }

        auditService.logSuccess(user, "FORGOT_PASSWORD_REQUEST", "User", user.getId().toString(), "Reset link sent");
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);

        if (verificationToken == null) {
            throw new RuntimeException("Invalid reset token.");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        auditService.logSuccess(user, AuditLog.Actions.PASSWORD_CHANGE, "User", user.getId().toString(), "Password reset via email link");
    }
}