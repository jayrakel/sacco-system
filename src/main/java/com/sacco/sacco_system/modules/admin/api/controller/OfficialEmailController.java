package com.sacco.sacco_system.modules.admin.api.controller;

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/official-emails")
@RequiredArgsConstructor
public class OfficialEmailController {

    private final UserRepository userRepository;

    /**
     * Assign official SACCO email to a user
     * Only admins can do this
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignOfficialEmail(
            @RequestParam UUID userId,
            @RequestParam String officialEmail) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate email format
            if (!officialEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new RuntimeException("Invalid email format");
            }

            // Check if official email already exists
            if (userRepository.findByOfficialEmail(officialEmail).isPresent()) {
                throw new RuntimeException("This official email is already assigned to another user");
            }

            // Only non-MEMBER roles can have official emails
            if (user.getRole() == User.Role.MEMBER) {
                throw new RuntimeException("Cannot assign official email to regular members");
            }

            user.setOfficialEmail(officialEmail);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Official email assigned successfully");
            response.put("userId", user.getId());
            response.put("personalEmail", user.getEmail());
            response.put("officialEmail", user.getOfficialEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get user's email configuration
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserEmails(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", user.getId());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("personalEmail", user.getEmail());
            response.put("officialEmail", user.getOfficialEmail());
            response.put("memberNumber", user.getMemberNumber());
            response.put("role", user.getRole());
            response.put("hasOfficialEmail", user.getOfficialEmail() != null);
            response.put("hasMemberAccess", user.getMemberNumber() != null);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Generate suggested official email based on role
     */
    @GetMapping("/suggest/{userId}")
    public ResponseEntity<Map<String, Object>> suggestOfficialEmail(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String saccoEmail = System.getenv("SACCO_EMAIL_DOMAIN");
            if (saccoEmail == null || saccoEmail.isEmpty()) {
                saccoEmail = "sacco.com"; // Default
            }

            String suggested = user.getRole().name().toLowerCase() + "@" + saccoEmail;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestedEmail", suggested);
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Remove official email (revert to personal email only)
     */
    @DeleteMapping("/remove/{userId}")
    public ResponseEntity<Map<String, Object>> removeOfficialEmail(@PathVariable UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setOfficialEmail(null);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Official email removed successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

