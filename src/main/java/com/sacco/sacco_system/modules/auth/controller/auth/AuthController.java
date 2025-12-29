package com.sacco.sacco_system.modules.auth.controller.auth;

import com.sacco.sacco_system.modules.auth.dto.AuthRequest;
import com.sacco.sacco_system.modules.auth.dto.ChangePasswordRequest;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.auth.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        try {
            return ResponseEntity.ok(authService.register(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("verify your email")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(request);
            return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.forgotPassword(payload.get("email"));
            // Always return success to prevent email scanning
            return ResponseEntity.ok(Map.of("success", true, "message", "If an account exists, a reset link has been sent."));
        } catch (Exception e) {
            // Log real error internally, return generic success externally
            return ResponseEntity.ok(Map.of("success", true, "message", "If an account exists, a reset link has been sent."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            authService.resetPassword(payload.get("token"), payload.get("newPassword"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully. Please login."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@AuthenticationPrincipal User user) {
        try {
            authService.logout(user);
            // Log successful logout (audit logging happens in AuthService)
            return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Logged out")); // Always return success for logout
        }
    }
}
