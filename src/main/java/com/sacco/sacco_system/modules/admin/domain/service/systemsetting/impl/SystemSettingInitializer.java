package com.sacco.sacco_system.modules.admin.domain.service.systemsetting.impl;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemSettingInitializer {

    private final SystemSettingRepository repository;

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            entry("REGISTRATION_FEE", "1000"),
            entry("MIN_MONTHLY_CONTRIBUTION", "500"),
            entry("LOAN_INTEREST_RATE", "10"),
            entry("LOAN_GRACE_PERIOD_WEEKS", "1"),
            entry("LOAN_LIMIT_MULTIPLIER", "3"),
            entry("LOAN_APPLICATION_FEE", "500"),
            entry("MIN_SAVINGS_FOR_LOAN", "5000"),
            entry("MIN_MONTHS_MEMBERSHIP", "3"),
            entry("MIN_SHARE_CAPITAL", "1000"),
            entry("MAX_ACTIVE_LOANS", "1"),
            entry("MAX_DEBT_RATIO", "0.66"),
            entry("MIN_SAVINGS_TO_GUARANTEE", "10000"),
            entry("MIN_MONTHS_TO_GUARANTEE", "6"),
            entry("MAX_GUARANTOR_LIMIT_RATIO", "2"),
            entry("SHARE_VALUE", "100"),
            entry("SACCO_NAME", "Secure Sacco"),
            entry("SACCO_TAGLINE", "Empowering Your Future"),
            entry("SACCO_ADDRESS", "P.O. Box 12345-00100, Nairobi, Kenya"),
            entry("SACCO_PHONE", "+254 700 000 000"),
            entry("SACCO_EMAIL", "info@securesacco.com"),
            entry("SACCO_WEBSITE", "https://www.securesacco.com"),
            entry("SACCO_LOGO", ""),
            entry("SACCO_FAVICON", ""),
            entry("BRAND_COLOR_PRIMARY", "#059669"),
            entry("BRAND_COLOR_SECONDARY", "#0f172a"),
            entry("BANK_NAME", "Co-operative Bank"),
            entry("BANK_ACCOUNT_NAME", "Sacco Main Account"),
            entry("BANK_ACCOUNT_NUMBER", "01100000000000"),
            entry("PAYBILL_NUMBER", "400200"),
            entry("LOAN_VOTING_METHOD", "MANUAL")
    );

    @PostConstruct
    public void initDefaults() {
        AtomicInteger count = new AtomicInteger(0);

        DEFAULTS.forEach((key, value) -> {
            if (repository.findByKey(key).isEmpty()) {
                String type = determineType(key);
                repository.save(SystemSetting.builder()
                        .key(key)
                        .value(value)
                        .description("System Default")
                        .dataType(type)
                        .build());
                count.incrementAndGet();
            }
        });

        if (count.get() > 0) {
            log.info("Initialized {} default system settings.", count.get());
        }
    }

    private String determineType(String key) {
        if (key.contains("SACCO") || key.contains("COLOR") || key.contains("BANK") ||
                key.contains("NAME") || key.contains("ADDRESS") || key.contains("EMAIL") ||
                key.contains("WEBSITE") || key.contains("METHOD")) {
            return "STRING";
        }
        return "NUMBER";
    }
}