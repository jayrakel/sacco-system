package com.sacco.sacco_system.modules.auth.service;

import com.sacco.sacco_system.modules.auth.dto.AuthRequest;
import com.sacco.sacco_system.modules.auth.dto.AuthResponse;
import com.sacco.sacco_system.modules.auth.dto.ChangePasswordRequest;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
// Custom JWT service
import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ 1. ADDED IMPORT
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // ✅ 2. ADDED ANNOTATION (Fixes "log cannot be resolved")
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public Map<String, Object> register(User user) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        if (user.getRole() == null) user.setRole(User.Role.MEMBER);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser.getEmail());

        String verificationToken = UUID.randomUUID().toString();
        VerificationToken vToken = VerificationToken.builder()
            .token(verificationToken)
            .user(savedUser)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .build();
        tokenRepository.save(vToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        return Map.of(
                "success", true,
                "message", "User registered successfully. Please check email for verification.",
                "token", token
        );
    }

    public AuthResponse login(AuthRequest request) {
        String emailOrUsername = request.getEmailOrUsername();
        
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailOrUsername, request.getPassword())
            );

            User user = userRepository.findByEmailOrOfficialEmail(emailOrUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
                auditService.logFailure(user, AuditLog.Actions.LOGIN, "User", user.getId().toString(), 
                    "Login attempt with unverified email", "Email not verified");
                throw new RuntimeException("Access Denied: Please verify your email first.");
            }

            String loginEmail = emailOrUsername;
            boolean isOfficialLogin = loginEmail.equals(user.getOfficialEmail());
            boolean isMemberLogin = loginEmail.equals(user.getEmail());

            if (user.getOfficialEmail() == null && user.getRole() != User.Role.MEMBER) {
                isOfficialLogin = true;
                isMemberLogin = false;
            }

            auditService.logSuccess(user, AuditLog.Actions.LOGIN, "User", user.getId().toString(), 
                String.format("Successful login as %s using %s", user.getRole(), loginEmail));

            String token = jwtService.generateToken(user.getEmail());

            boolean setupRequired = false;
            if (user.getRole() == User.Role.ADMIN) {
                if (!userRepository.existsByRole(User.Role.CHAIRPERSON)) {
                    setupRequired = true;
                }
            }

            return AuthResponse.builder()
                    .token(token)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().toString())
                    .mustChangePassword(user.isMustChangePassword())
                    .systemSetupRequired(setupRequired)
                    .isOfficialLogin(isOfficialLogin)
                    .isMemberLogin(isMemberLogin)
                    .build();
        } catch (Exception e) {
            auditService.logFailure(null, AuditLog.Actions.LOGIN, "User", emailOrUsername, 
                "Failed login attempt", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            auditService.logFailure(currentUser, AuditLog.Actions.PASSWORD_CHANGE, "User", 
                currentUser.getId().toString(), "Password change failed", "Incorrect current password");
            throw new RuntimeException("Current password is incorrect");
        }
        
        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);
        
        auditService.logSuccess(currentUser, AuditLog.Actions.PASSWORD_CHANGE, "User", 
            currentUser.getId().toString(), "Password changed successfully");
    }

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

    // ✅ FIXED: Robust "Forgot Password" to prevent Invalid Token error
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmailOrOfficialEmail(email)
                .orElseThrow(() -> new RuntimeException("If that email exists, a reset link has been sent."));

        // 1. Manually check for existing token
        VerificationToken existingToken = tokenRepository.findByUser(user);
        if (existingToken != null) {
            tokenRepository.delete(existingToken);
            tokenRepository.flush(); // Force DB to delete immediately to prevent conflicts
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
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        auditService.logSuccess(user, AuditLog.Actions.PASSWORD_CHANGE, "User", user.getId().toString(), "Password reset via email link");
    }

    public void logout(User user) {
        if (user != null) {
            auditService.logSuccess(user, AuditLog.Actions.LOGOUT, "User", 
                user.getId().toString(), "User logged out successfully");
        }
    }
}