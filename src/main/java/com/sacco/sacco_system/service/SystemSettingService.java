package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.SystemSetting;
import com.sacco.sacco_system.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final String UPLOAD_DIR = "uploads/settings/";

    private static final Map<String, String> DEFAULTS = Map.of(
            "REGISTRATION_FEE", "1000",
            "MIN_MONTHLY_CONTRIBUTION", "500",
            "LOAN_INTEREST_RATE", "12",
            "LOAN_LIMIT_MULTIPLIER", "3",
            "SACCO_NAME", "Sacco System",
            "SACCO_TAGLINE", "Empowering Your Future",
            "SACCO_LOGO", "",
            "SACCO_FAVICON", "",
            "BRAND_COLOR_PRIMARY", "#059669",   // Emerald 600
            "BRAND_COLOR_SECONDARY", "#0f172a"  // Slate 900
    );

    @PostConstruct
    public void initDefaults() {
        DEFAULTS.forEach((key, value) -> {
            if (repository.findByKey(key).isEmpty()) {
                String type = key.contains("SACCO") ? "STRING" : "NUMBER"; // Simple type inference
                repository.save(SystemSetting.builder()
                        .key(key)
                        .value(value)
                        .description("System Default")
                        .dataType(type)
                        .build());
            }
        });
    }

    public List<SystemSetting> getAllSettings() {
        return repository.findAll();
    }

    @Transactional
    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        setting.setValue(value);
        return repository.save(setting);
    }

    // ✅ NEW: Handle Image Uploads for Settings
    @Transactional
    public SystemSetting uploadSettingImage(String key, MultipartFile file) throws IOException {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));

        // Create directory
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename to avoid caching issues
        String filename = key + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);

        // Delete old file if exists (optional cleanup)
        if (setting.getValue() != null && !setting.getValue().isEmpty()) {
            try {
                Files.deleteIfExists(uploadPath.resolve(setting.getValue()));
            } catch (Exception e) {
                // Ignore delete errors
            }
        }

        // Save new file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save relative path to DB
        setting.setValue(filename);
        return repository.save(setting);
    }

    public double getDouble(String key) {
        return Double.parseDouble(repository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse("0"));
    }
}