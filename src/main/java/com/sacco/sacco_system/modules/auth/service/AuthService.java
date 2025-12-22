package com.sacco.sacco_system.modules.auth.service;


import com.sacco.sacco_system.modules.auth.dto.AuthRequest;
import com.sacco.sacco_system.modules.auth.dto.AuthResponse;
import com.sacco.sacco_system.modules.auth.dto.ChangePasswordRequest;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.model.VerificationToken;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.auth.repository.VerificationTokenRepository;
import com.sacco.sacco_system.modules.auth.service.JwtService;
// Custom JWT service - create if missing
import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    // @Loggable removed - create separate audit service
    public Map<String, Object> register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Default role if not provided
        if (user.getRole() == null) user.setRole(User.Role.MEMBER);

        User savedUser = userRepository.save(user);

        // Generate Token
        String token = jwtService.generateToken(savedUser.getEmail()); // TODO: Fixed type - was passing User instead of String

        // Send Verification Email
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

            // Find user by either personal or official email
            User user = userRepository.findByEmailOrOfficialEmail(emailOrUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
                auditService.logFailure(user, AuditLog.Actions.LOGIN, "User", user.getId().toString(), 
                    "Login attempt with unverified email", "Email not verified");
                throw new RuntimeException("Access Denied: Please verify your email first.");
            }

            // Determine portal access based on which email was used
            String loginEmail = emailOrUsername;
            boolean isOfficialLogin = loginEmail.equals(user.getOfficialEmail());
            boolean isMemberLogin = loginEmail.equals(user.getEmail());

            // FLEXIBLE LOGIN: If official email not set, allow admin access with personal email
            if (user.getOfficialEmail() == null && user.getRole() != User.Role.MEMBER) {
                // No official email set - allow admin access with personal email (for testing/setup)
                isOfficialLogin = true;
                isMemberLogin = false;
            }

            // Log successful login
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
                .memberNumber(user.getMemberNumber())
                .role(user.getRole().toString())
                .mustChangePassword(user.isMustChangePassword())
                .systemSetupRequired(setupRequired)
                .isOfficialLogin(isOfficialLogin) // NEW: Tells frontend which dashboard to show
                .isMemberLogin(isMemberLogin)
                .build();
        } catch (Exception e) {
            // Log failed login attempt
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
        
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);
        
        // Log successful password change
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
        
        // Delete used token
        tokenRepository.delete(verificationToken);
        
        // Log successful verification
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
        
        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);
        
        // Generate new verification token
        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
            .token(tokenString)
            .user(user)
            .expiryDate(LocalDateTime.now().plusHours(24))
            .build();
        tokenRepository.save(verificationToken);
        
        // Send new verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), tokenString);
        
        // Log action
        auditService.logSuccess(user, "RESEND_VERIFICATION", "User", 
            user.getId().toString(), "Verification email resent");
    }

    public void logout(User user) {
        if (user != null) {
            auditService.logSuccess(user, AuditLog.Actions.LOGOUT, "User", 
                user.getId().toString(), "User logged out successfully");
        }
    }
}



