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

    // ✅ Inject the 3 specialized handlers
    private final SystemSettingReadHandler readHandler;
    private final SystemSettingWriteHandler writeHandler;
    private final SystemSettingFileHandler fileHandler;

    // --- READ ENDPOINTS ---

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAllSettings() {
        return readHandler.getAllSettings();
    }

    // --- WRITE ENDPOINTS ---

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createOrUpdateSetting(@RequestBody Map<String, String> body) {
        return writeHandler.createOrUpdateSetting(body);
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Map<String, String> body) {
        return writeHandler.updateSetting(key, body);
    }

    // --- FILE ENDPOINTS ---

    @PostMapping(value = "/upload/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> uploadSettingImage(
            @PathVariable String key,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return fileHandler.uploadSettingImage(key, file);
    }
}