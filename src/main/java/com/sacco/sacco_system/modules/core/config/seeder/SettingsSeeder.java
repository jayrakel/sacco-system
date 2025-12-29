package com.sacco.sacco_system.modules.core.config.seeder;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run first
public class SettingsSeeder {

    private final SystemSettingRepository systemSettingRepository;

    @Transactional
    public void seed() {
        if (systemSettingRepository.count() > 0) {
            return;
        }

        log.info("Seeding default system settings...");

        List<SystemSetting> settings = Arrays.asList(
                create("sacco_name", "Seccure Sacco", "Organization Name", "STRING"),
                create("registration_fee", "1000", "One-time mandatory fee (KES)", "DECIMAL"),
                create("min_share_capital", "2000", "Minimum shares to fully join (KES)", "DECIMAL"),
                create("min_weekly_deposit", "500", "Minimum saving per week/month (KES)", "DECIMAL"),
                create("penalty_missed_savings", "50", "Penalty for missed contribution", "DECIMAL"),
                create("min_savings_for_loan", "5000", "Min savings required to apply for a loan", "DECIMAL"),
                create("loan_interest_rate", "12", "Default Annual interest rate (%)", "DECIMAL"),
                create("loan_multiplier", "3", "Loan limit multiplier (x Savings)", "INTEGER"),
                create("loan_processing_fee", "500", "Standard processing fee (KES)", "DECIMAL"),
                create("min_guarantors", "2", "Minimum guarantors required", "INTEGER")
        );

        systemSettingRepository.saveAll(settings);
    }

    private SystemSetting create(String key, String value, String desc, String type) {
        return SystemSetting.builder()
                .key(key)
                .value(value)
                .description(desc)
                .dataType(type)
                .build();
    }
}