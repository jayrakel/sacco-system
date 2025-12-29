package com.sacco.sacco_system.modules.admin.api.controller.officialemail;

import com.sacco.sacco_system.modules.admin.api.controller.officialemail.handler.OfficialEmailReadHandler;
import com.sacco.sacco_system.modules.admin.api.controller.officialemail.handler.OfficialEmailWriteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/official-emails")
@RequiredArgsConstructor
public class OfficialEmailController {

    private final OfficialEmailReadHandler readHandler;
    private final OfficialEmailWriteHandler writeHandler;

    /**
     * Assign official SACCO email to a user
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> assignOfficialEmail(
            @RequestParam UUID userId,
            @RequestParam String officialEmail) {
        return writeHandler.assignOfficialEmail(userId, officialEmail);
    }

    /**
     * Get user's email configuration
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getUserEmails(@PathVariable UUID userId) {
        return readHandler.getUserEmails(userId);
    }

    /**
     * Generate suggested official email based on role
     */
    @GetMapping("/suggest/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> suggestOfficialEmail(@PathVariable UUID userId) {
        return readHandler.suggestOfficialEmail(userId);
    }

    /**
     * Remove official email (revert to personal email only)
     */
    @DeleteMapping("/remove/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> removeOfficialEmail(@PathVariable UUID userId) {
        return writeHandler.removeOfficialEmail(userId);
    }
}