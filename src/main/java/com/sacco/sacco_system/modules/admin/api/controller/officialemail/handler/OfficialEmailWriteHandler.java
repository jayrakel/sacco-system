package com.sacco.sacco_system.modules.admin.api.controller.officialemail.handler;

import com.sacco.sacco_system.modules.admin.domain.service.officialemail.OfficialEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfficialEmailWriteHandler {

    private final OfficialEmailService service;

    public ResponseEntity<?> assignOfficialEmail(UUID userId, String officialEmail) {
        if (officialEmail == null || officialEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Official email is required");
        }

        Map<String, Object> result = service.assignOfficialEmail(userId, officialEmail.trim());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Official email assigned successfully",
                "data", result
        ));
    }

    public ResponseEntity<?> removeOfficialEmail(UUID userId) {
        service.removeOfficialEmail(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Official email removed successfully"
        ));
    }
}