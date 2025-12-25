package com.sacco.sacco_system.modules.admin.domain.service;

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

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;
    private final String UPLOAD_DIR = "uploads/settings/";

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            entry("REGISTRATION_FEE", "1000"),
            entry("MIN_MONTHLY_CONTRIBUTION", "500"),
            entry("LOAN_INTEREST_RATE", "10"),
            entry("LOAN_GRACE_PERIOD_WEEKS", "1"),
            entry("LOAN_LIMIT_MULTIPLIER", "3"),
            entry("LOAN_APPLICATION_FEE", "500"),
            // Loan Eligibility Thresholds
            entry("MIN_SAVINGS_FOR_LOAN", "5000"),
            entry("MIN_MONTHS_MEMBERSHIP", "3"),
            entry("MIN_SHARE_CAPITAL", "1000"),
            // Guarantor Eligibility Thresholds
            entry("MIN_SAVINGS_TO_GUARANTEE", "10000"),
            entry("MIN_MONTHS_TO_GUARANTEE", "6"),
            entry("MAX_GUARANTOR_LIMIT_RATIO", "2"),
            // Share Capital
            entry("SHARE_VALUE", "100"),
            // Branding & Contact Details
            entry("SACCO_NAME", "Secure Sacco"),
            entry("SACCO_TAGLINE", "Empowering Your Future"),
            entry("SACCO_ADDRESS", "P.O. Box 12345-00100, Nairobi, Kenya"), // NEW
            entry("SACCO_PHONE", "+254 700 000 000"), // NEW
            entry("SACCO_EMAIL", "info@securesacco.com"), // NEW
            entry("SACCO_WEBSITE", "https://www.securesacco.com"), // NEW
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
                // Determine data type for the frontend
                String type = "NUMBER";
                if (key.contains("SACCO") || key.contains("COLOR") || key.contains("BANK") || key.contains("NAME")) {
                    type = "STRING";
                }
                
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

    public String getString(String key, String defaultValue) {
    return repository.findByKey(key)
            .map(SystemSetting::getValue)
            .orElse(defaultValue);
}

    @Transactional
    public SystemSetting createOrUpdate(String key, String value, String description) {
        return repository.findByKey(key)
                .map(existing -> {
                    existing.setValue(value);
                    if (description != null && !description.isEmpty()) {
                        existing.setDescription(description);
                    }
                    return repository.save(existing);
                })
                .orElseGet(() -> {
                    // Determine type automatically
                    String type = "STRING";
                    try {
                        Double.parseDouble(value);
                        type = "NUMBER";
                    } catch (NumberFormatException e) {
                        // Keep as STRING
                    }

                    return repository.save(SystemSetting.builder()
                            .key(key)
                            .value(value)
                            .description(description)
                            .dataType(type)
                            .build());
                });
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

    public double getDouble(String key, double defaultValue) {
        return repository.findByKey(key)
                .map(s -> {
                    try {
                        return Double.parseDouble(s.getValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}