package com.sacco.sacco_system.modules.admin.api.controller;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAllSettings()));
    }

    // âœ… ADD THIS METHOD: Handles the "Save" button from SystemSettings.jsx
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrUpdateSetting(@RequestBody Map<String, String> body) {
        try {
            String key = body.get("key");
            String value = body.get("value");
            String description = body.get("description");

            if (key == null || value == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Key and Value are required"));
            }

            // Convert key to uppercase snake_case (e.g. "My Setting" -> "MY_SETTING")
            String formattedKey = key.trim().toUpperCase().replace(" ", "_");

            SystemSetting updated = service.createOrUpdate(formattedKey, value, description);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Setting saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateSetting(@PathVariable String key, @RequestBody Map<String, String> body) {
        try {
            SystemSetting updated = service.updateSetting(key, body.get("value"));
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Setting updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // âœ… NEW: Upload Endpoint
    @PostMapping(value = "/upload/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadSettingImage(
            @PathVariable String key,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            SystemSetting updated = service.uploadSettingImage(key, file);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "Image uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}



