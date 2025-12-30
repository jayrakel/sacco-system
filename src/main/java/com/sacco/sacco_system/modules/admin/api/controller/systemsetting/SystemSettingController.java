package com.sacco.sacco_system.modules.admin.api.controller.systemsetting;

import com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler.SystemSettingFileHandler;
import com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler.SystemSettingReadHandler;
import com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler.SystemSettingWriteHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingReadHandler readHandler;
    private final SystemSettingWriteHandler writeHandler;
    private final SystemSettingFileHandler fileHandler;

    // --- READ ENDPOINTS ---

    @GetMapping
    @PreAuthorize("isAuthenticated()") // ✅ Correct: Allows all logged-in users (Members/Admins)
    public ResponseEntity<?> getAllSettings() {
        return readHandler.getAllSettings();
    }

    // --- WRITE ENDPOINTS (Text/JSON) ---

    @PostMapping
    // ✅ FIX: Updated roles to match your actual system roles
    @PreAuthorize("hasRole('ADMIN', 'CHAIRPERSON', 'TREASURER')")
    public ResponseEntity<?> createOrUpdateSetting(@RequestBody Map<String, String> body) {
        return writeHandler.createOrUpdateSetting(body);
    }

    @PutMapping("/{key}")
    // ✅ FIX: Updated roles to match your actual system roles
    @PreAuthorize("hasRole('ADMIN', 'CHAIRPERSON', 'TREASURER')")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Map<String, String> body) {
        return writeHandler.updateSetting(key, body);
    }

    // --- FILE UPLOAD ENDPOINTS ---

    /**
     * Endpoint for uploading Logos/Favicons.
     * Frontend MUST send a POST request to: /api/settings/upload/{key}
     * Form Data Key MUST be: "file"
     */
    @PostMapping(value = "/upload/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN', 'CHAIRPERSON', 'TREASURER')")
    public ResponseEntity<?> uploadSettingImage(
            @PathVariable String key,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return fileHandler.uploadSettingImage(key, file);
    }
}