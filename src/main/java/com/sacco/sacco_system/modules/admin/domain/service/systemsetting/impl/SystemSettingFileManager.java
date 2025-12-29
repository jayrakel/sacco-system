package com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl;

import com.sacco.sacco_system.annotation.Loggable; // ✅ Import the annotation
import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SystemSettingFileManager {

    private final SystemSettingRepository repository;
    // ❌ REMOVED: private final AuditService auditService; (Not needed anymore!)
    private final String UPLOAD_DIR = "uploads/settings/";

    @Transactional
    @Loggable(action = "UPLOAD_SETTING_IMAGE", category = "ADMIN") // ✅ Auto-Audit enabled
    public SystemSetting uploadSettingImage(String key, MultipartFile file) throws IOException {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = key + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);

        if (setting.getValue() != null && !setting.getValue().isEmpty()) {
            try { Files.deleteIfExists(uploadPath.resolve(setting.getValue())); } catch (Exception ignored) {}
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        setting.setValue(filename);

        return repository.save(setting);
    }
}