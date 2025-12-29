package com.sacco.sacco_system.modules.core.config;

import com.sacco.sacco_system.modules.core.config.seeder.AccountingSeeder;
import com.sacco.sacco_system.modules.core.config.seeder.AdminUserSeeder;
import com.sacco.sacco_system.modules.core.config.seeder.SettingsSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final SettingsSeeder settingsSeeder;
    private final AdminUserSeeder adminUserSeeder;
    private final AccountingSeeder accountingSeeder;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("Starting system initialization...");

            try {
                // 1. Settings
                settingsSeeder.seed();

                // 2. Admin User
                adminUserSeeder.seed();

                // 3. Accounting
                accountingSeeder.seed();

                log.info("System initialization completed successfully.");
            } catch (Exception e) {
                log.error("System initialization failed: {}", e.getMessage(), e);
            }
        };
    }
}