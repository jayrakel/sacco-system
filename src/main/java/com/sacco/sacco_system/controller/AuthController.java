package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.AuthRequest;
import com.sacco.sacco_system.dto.AuthResponse;
import com.sacco.sacco_system.dto.ChangePasswordRequest;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.UserRepository;
import com.sacco.sacco_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        // 1. Encode the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Set default role if missing
        if (user.getRole() == null) {
            user.setRole(User.Role.MEMBER);
        }

        // 3. Save User
        User savedUser = userRepository.save(user);

        // 4. Generate Token
        String token = jwtService.generateToken(savedUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // 1. Authenticate (Checks password automatically)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. Fetch User
        var user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Add Role to Token Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().toString());

        var token = jwtService.generateToken(claims, user);

        // 4. Build Response with 'mustChangePassword' flag
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().toString())
                .mustChangePassword(user.isMustChangePassword()) // Notify frontend
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        // 1. Get currently logged-in user
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Verify old password
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Current password is incorrect");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 3. Update to new password
        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false); // Disable the flag
        userRepository.save(currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password changed successfully");

        return ResponseEntity.ok(response);
    }
}