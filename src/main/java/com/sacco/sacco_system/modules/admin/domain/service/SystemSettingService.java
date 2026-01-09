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

    // ✅ MERGED DEFAULTS: Consolidated all settings here
    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            // Fees & Contributions
            entry("REGISTRATION_FEE", "1000"),
            entry("MIN_MONTHLY_CONTRIBUTION", "500"),
            entry("MIN_WEEKLY_DEPOSIT", "500"), // Added from DataInitializer
            entry("PENALTY_MISSED_SAVINGS", "50"), // Added from DataInitializer
            entry("MIN_SHARE_CAPITAL", "2000"), // Updated to 2000 to match DataInitializer

            // Savings
            entry("MINIMUM_SAVINGS_BALANCE", "50000.00"), // Added (Used for Product creation)

            // Loans
            entry("LOAN_INTEREST_RATE", "12"), // Updated to 12%
            entry("LOAN_GRACE_PERIOD_WEEKS", "1"),
            entry("LOAN_LIMIT_MULTIPLIER", "3"),
            entry("LOAN_APPLICATION_FEE", "500"), // Standardized name (was loan_processing_fee)
            entry("LOAN_PROCESSING_FEE", "500"), // Kept for backward compatibility if needed
            entry("MIN_SAVINGS_FOR_LOAN", "5000"),
            entry("MIN_MONTHS_MEMBERSHIP", "3"),
            entry("MIN_GUARANTORS", "2"), // Added

            // Governance
            entry("MAX_ACTIVE_LOANS", "1"),
            entry("MAX_DEBT_RATIO", "0.66"),
            entry("LOAN_VOTING_METHOD", "MANUAL"),

            // Guarantor Eligibility
            entry("MIN_SAVINGS_TO_GUARANTEE", "10000"),
            entry("MIN_MONTHS_TO_GUARANTEE", "6"),
            entry("MAX_GUARANTOR_LIMIT_RATIO", "2"),

            // Shares
            entry("SHARE_VALUE", "100"),

            // Branding & Contact
            entry("SACCO_NAME", "Seccure Sacco"),
            entry("SACCO_TAGLINE", "Empowering Your Future"),
            entry("SACCO_ADDRESS", "P.O. Box 12345-00100, Nairobi, Kenya"),
            entry("SACCO_PHONE", "+254 700 000 000"),
            entry("SACCO_EMAIL", "info@securesacco.com"),
            entry("SACCO_WEBSITE", "https://www.securesacco.com"),
            entry("SACCO_LOGO", ""),
            entry("SACCO_FAVICON", ""),
            entry("BRAND_COLOR_PRIMARY", "#059669"),
            entry("BRAND_COLOR_SECONDARY", "#0f172a"),

            // Banking & GL
            entry("BANK_NAME", "Co-operative Bank"),
            entry("BANK_ACCOUNT_NAME", "Sacco Main Account"),
            entry("BANK_ACCOUNT_NUMBER", "01100000000000"),
            entry("PAYBILL_NUMBER", "400200"),
            entry("DEFAULT_BANK_GL_CODE", "1012") // Added
    );

    @PostConstruct
    public void initDefaults() {
        DEFAULTS.forEach((key, value) -> {
            // Only create if key doesn't exist (prevents overwriting custom changes)
            if (repository.findByKey(key).isEmpty()) {
                String type = "NUMBER";
                if (key.contains("SACCO") || key.contains("COLOR") || key.contains("BANK") || key.contains("NAME") || key.contains("ADDRESS") || key.contains("EMAIL") || key.contains("WEBSITE") || key.contains("CODE") || key.contains("METHOD")) {
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

    // ... [Keep your existing getter/setter methods below] ...

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

    // Overload for single argument
    public String getString(String key) {
        return repository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(null);
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
            } catch (Exception e) { }
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