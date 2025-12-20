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
import com.sacco.sacco_system.modules.notification.domain.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        VerificationToken vToken = new VerificationToken(savedUser, verificationToken);
        tokenRepository.save(vToken);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        return Map.of(
                "success", true,
                "message", "User registered successfully. Please check email for verification.",
                "token", token
        );
    }
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // Find user by either personal or official email
        User user = userRepository.findByEmailOrOfficialEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Access Denied: Please verify your email first.");
        }

        // Determine portal access based on which email was used
        String loginEmail = request.getUsername();
        boolean isOfficialLogin = loginEmail.equals(user.getOfficialEmail());
        boolean isMemberLogin = loginEmail.equals(user.getEmail());

        // FLEXIBLE LOGIN: If official email not set, allow admin access with personal email
        if (user.getOfficialEmail() == null && user.getRole() != User.Role.MEMBER) {
            // No official email set - allow admin access with personal email (for testing/setup)
            isOfficialLogin = true;
            isMemberLogin = false;
        }

        // Validate access
        if (isOfficialLogin && user.getRole() == User.Role.MEMBER) {
            throw new RuntimeException("This email is not authorized for administrative access");
        }

        // Only enforce official email requirement if one is set
        if (isMemberLogin && user.getRole() != User.Role.MEMBER && user.getMemberNumber() == null && user.getOfficialEmail() != null) {
            throw new RuntimeException("This email is for member access only. Use your official SACCO email for administrative access");
        }

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
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        userRepository.save(currentUser);
    }
}



