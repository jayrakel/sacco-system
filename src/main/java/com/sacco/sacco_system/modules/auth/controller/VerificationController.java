package com.sacco.sacco_system.modules.auth.controller;

import com.sacco.sacco_system.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VerificationController {

    private final AuthService authService;

    @RequestMapping(value = "/verify/email", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            // Get token from query param or request body
            String token = tokenParam != null ? tokenParam : (body != null ? body.get("token") : null);
            
            if (token == null) {
                throw new RuntimeException("Token parameter is required");
            }
            
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("success", true, "message", "Email verified successfully. You can now login."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Email is required");
            }
            
            authService.resendVerificationEmail(email);
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Verification email has been resent. Please check your inbox."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, 
                "message", e.getMessage()
            ));
        }
    }
}
