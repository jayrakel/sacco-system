package com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemSettingFileHandler {

    private final SystemSettingService service;

    // Strict Allowed Types (Whitelist approach)
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/x-icon",
            "image/vnd.microsoft.icon"
    );

    public ResponseEntity<?> uploadSettingImage(String key, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }

        // Strict Security: Validate MIME Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only PNG, JPEG, and ICO images are allowed.");
        }

        // Strict Security: File Size Limit (e.g., 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 5MB.");
        }

        SystemSetting updated = service.uploadSettingImage(key, file);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", updated,
                "message", "Image uploaded successfully"
        ));
    }
}