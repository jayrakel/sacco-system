package com.sacco.sacco_system.modules.admin.api.controller.officialemail.handler;

import com.sacco.sacco_system.modules.admin.domain.service.officialemail.OfficialEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OfficialEmailReadHandler {

    private final OfficialEmailService service;

    public ResponseEntity<?> getUserEmails(UUID userId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", service.getUserEmailDetails(userId)
        ));
    }

    public ResponseEntity<?> suggestOfficialEmail(UUID userId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", service.generateSuggestion(userId)
        ));
    }
}