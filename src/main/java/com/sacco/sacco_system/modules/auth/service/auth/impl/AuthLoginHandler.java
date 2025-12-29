package com.sacco.sacco_system.modules.auth.service.auth.impl;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.auth.dto.AuthRequest;
import com.sacco.sacco_system.modules.auth.dto.AuthResponse;
import com.sacco.sacco_system.modules.auth.service.jwt.JwtService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLoginHandler {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthResponse login(AuthRequest request) {
        String emailOrUsername = request.getEmailOrUsername();

        try {
            // 1. Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(emailOrUsername, request.getPassword())
            );

            // 2. Fetch User
            User user = userRepository.findByEmailOrOfficialEmail(emailOrUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3. Check Verification
            if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
                auditService.logFailure(user, AuditLog.Actions.LOGIN, "User", user.getId().toString(),
                        "Login attempt with unverified email", "Email not verified");
                throw new RuntimeException("Access Denied: Please verify your email first.");
            }

            // 4. Determine Login Type
            String loginEmail = emailOrUsername;
            boolean isOfficialLogin = loginEmail.equals(user.getOfficialEmail());
            boolean isMemberLogin = loginEmail.equals(user.getEmail());

            if (user.getOfficialEmail() == null && user.getRole() != User.Role.MEMBER) {
                isOfficialLogin = true;
                isMemberLogin = false;
            }

            // 5. Audit Success
            auditService.logSuccess(user, AuditLog.Actions.LOGIN, "User", user.getId().toString(),
                    String.format("Successful login as %s using %s", user.getRole(), loginEmail));

            // 6. Generate Token
            String token = jwtService.generateToken(user.getEmail());

            // 7. Check System Setup (For Admins)
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

    public void logout(User user) {
        if (user != null) {
            auditService.logSuccess(user, AuditLog.Actions.LOGOUT, "User",
                    user.getId().toString(), "User logged out successfully");
        }
    }
}