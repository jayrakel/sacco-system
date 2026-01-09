package com.sacco.sacco_system.modules.core.config;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.service.UserService;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
// Removed SystemSettingRepository import as it's no longer used directly here
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserService userService;
    private final AccountingService accountingService;
    // Removed systemSettingRepository

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Value("${app.default-admin.phone}")
    private String adminPhone;

    @Value("${app.official-email-domain}")
    private String officialEmailDomain;

    @Bean
    @Transactional
    public CommandLineRunner initializeData() {
        return _ -> {
            log.info("🚀 Starting system initialization...");

            try {
                // 1. Initialize System Settings - Handled by SystemSettingService @PostConstruct

                // 2. Initialize Default Admin User
                initializeAdminUser();

                // 3. Initialize GL Accounts and Mappings
                accountingService.initChartOfAccounts();
                accountingService.initDefaultMappings();

                log.info("✅ System initialization completed successfully!");
            } catch (Exception e) {
                log.error("❌ System initialization failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize system", e);
            }
        };
    }

    private void initializeAdminUser() {
        try {
            userService.getUserByEmail(adminEmail);
            log.info("ℹ️ Admin user already exists: {}", adminEmail);
        } catch (Exception e) {
            log.info("👤 Creating default admin user...");

            String officialEmail = "admin@" + officialEmailDomain;

            userService.createBootstrapUser(
                    "System", "Administrator", adminEmail,
                    officialEmail, adminPhone, adminPassword, User.Role.ADMIN
            );
            log.info("✅ Default admin user created.");
        }
    }
}