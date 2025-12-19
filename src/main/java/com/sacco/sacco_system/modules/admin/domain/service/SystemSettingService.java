package com.sacco.sacco_system.modules.admin.domain.service;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
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
import java.util.Optional;
import java.util.UUID;

import static java.util.Map.entry; // Import for entry()

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final String UPLOAD_DIR = "uploads/settings/";

    // âœ… FIXED: Used Map.ofEntries to support > 10 items
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            entry("REGISTRATION_FEE", "1000"),
            entry("MIN_MONTHLY_CONTRIBUTION", "500"),
            entry("LOAN_INTEREST_RATE", "12"),
            entry("LOAN_GRACE_PERIOD_WEEKS", "1"),
            entry("LOAN_LIMIT_MULTIPLIER", "3"),
            entry("SACCO_NAME", "Sacco System"),
            entry("SACCO_TAGLINE", "Empowering Your Future"),
            entry("SACCO_LOGO", ""),
            entry("SACCO_FAVICON", ""),
            entry("BRAND_COLOR_PRIMARY", "#059669"),
            entry("BRAND_COLOR_SECONDARY", "#0f172a"),
            // Bank Details
            entry("BANK_NAME", "Co-operative Bank"),
            entry("BANK_ACCOUNT_NAME", "Sacco Main Account"),
            entry("BANK_ACCOUNT_NUMBER", "01100000000000"),
            entry("PAYBILL_NUMBER", "400200"),
            entry("LOAN_VOTING_METHOD", "MANUAL")
    );

    @PostConstruct
    public void initDefaults() {
        DEFAULTS.forEach((key, value) -> {
            if (repository.findByKey(key).isEmpty()) {
                String type = key.contains("SACCO") || key.contains("COLOR") || key.contains("BANK") || key.contains("NAME") ? "STRING" : "NUMBER";
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

    public Optional<String> getSetting(String key) {
        return repository.findByKey(key).map(SystemSetting::getValue);
    }

    @Transactional
    public SystemSetting updateSetting(String key, String value) {
        SystemSetting setting = repository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        setting.setValue(value);
        return repository.save(setting);
    }

    @Transactional
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
            try {
                Files.deleteIfExists(uploadPath.resolve(setting.getValue()));
            } catch (Exception e) {
                // Ignore delete errors
            }
        }

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        setting.setValue(filename);
        return repository.save(setting);
    }

    public double getDouble(String key) {
        return Double.parseDouble(repository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse("0"));
    }
}



