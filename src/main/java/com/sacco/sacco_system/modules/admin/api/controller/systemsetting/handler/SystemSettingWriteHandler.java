package com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemSettingWriteHandler {

    private final SystemSettingService service;

    public ResponseEntity<?> createOrUpdateSetting(Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        String description = body.get("description");

        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting Key is required.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Setting Value is required.");
        }

        // Strict: Normalize Key to prevent duplicates like "Sacco Name" vs "SACCO_NAME"
        String formattedKey = key.trim().toUpperCase().replace(" ", "_");

        SystemSetting updated = service.createOrUpdate(formattedKey, value, description);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", updated,
                "message", "Setting saved successfully"
        ));
    }

    public ResponseEntity<?> updateSetting(String key, Map<String, String> body) {
        String value = body.get("value");

        // Strict: Fail fast if no value provided
        if (value == null) {
            throw new IllegalArgumentException("Value is required for update.");
        }

        SystemSetting updated = service.updateSetting(key, value);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", updated,
                "message", "Setting updated successfully"
        ));
    }
}