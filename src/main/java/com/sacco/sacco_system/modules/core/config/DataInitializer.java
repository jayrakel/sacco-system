package com.sacco.sacco_system.modules.core.config;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.service.UserService;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserService userService;
    private final AccountingService accountingService;
    private final SystemSettingRepository systemSettingRepository;
    private final SavingsProductRepository savingsProductRepository; // ✅ Added Repository
    private final PasswordEncoder passwordEncoder;

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
        return args -> {
            log.info("🚀 Starting system initialization...");

            try {
                // 1. Initialize System Settings
                initializeSystemSettings();

                // 2. Initialize Default Admin User
                initializeAdminUser();

                // 3. Initialize GL Accounts and Mappings
                accountingService.initChartOfAccounts();
                accountingService.initDefaultMappings();

                // 4. ✅ Initialize Default Savings Product
                initializeDefaultSavingsProduct();

                log.info("✅ System initialization completed successfully!");
            } catch (Exception e) {
                log.error("❌ System initialization failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize system", e);
            }
        };
    }

    private void initializeDefaultSavingsProduct() {
        // Check if ANY savings product exists
        if (savingsProductRepository.count() > 0) {
            log.info("ℹ️ Savings products already exist, skipping default creation...");
            return;
        }

        log.info("💰 Creating default 'Ordinary Savings' product...");

        String MINIMUM_SAVINGS_BALANCE = systemSettingRepository.findByKey("MINIMUM_SAVINGS_BALANCE")
                .map(setting -> setting.getValue())
                .orElse("50000");

        SavingsProduct defaultSavings = SavingsProduct.builder()
                .productCode("SAV001") // Standard Code
                .productName("Ordinary Savings")
                .description("Default savings account for all members")
                .currencyCode("KES")
                .type(SavingsProduct.ProductType.SAVINGS)
                .interestRate(new BigDecimal("5.00")) // 5% Interest
                .minBalance(new BigDecimal(MINIMUM_SAVINGS_BALANCE))
                .minDurationMonths(0)
                .allowWithdrawal(true)
                .active(true)
                .build();

        savingsProductRepository.save(defaultSavings);
        log.info("✅ Default Savings Product 'SAV001' created.");
    }

    private void initializeSystemSettings() {
        if (systemSettingRepository.count() > 0) {
            log.info("ℹ️ System settings already initialized, skipping...");
            return;
        }

        log.info("⚙️ Seeding default system settings...");

        List<SystemSetting> settings = Arrays.asList(
                createSetting("sacco_name", "Seccure Sacco", "Organization Name", "STRING"),
                createSetting("registration_fee", "1000", "One-time mandatory fee (KES)", "DECIMAL"),
                createSetting("min_share_capital", "2000", "Minimum shares to fully join (KES)", "DECIMAL"),
                createSetting("min_weekly_deposit", "500", "Minimum saving per week/month (KES)", "DECIMAL"),
                createSetting("penalty_missed_savings", "50", "Penalty for missed contribution", "DECIMAL"),
                createSetting("min_savings_for_loan", "5000", "Min savings required to apply for a loan", "DECIMAL"),
                createSetting("loan_interest_rate", "12", "Default Annual interest rate (%)", "DECIMAL"),
                createSetting("loan_multiplier", "3", "Loan limit multiplier (x Savings)", "INTEGER"),
                createSetting("loan_processing_fee", "500", "Standard processing fee (KES)", "DECIMAL"),
                createSetting("min_guarantors", "2", "Minimum guarantors required", "INTEGER"),
                createSetting("DEFAULT_BANK_GL_CODE", "1010", "Default Bank GL Account", "STRING"), // Added for safety
                createSetting("MINIMUM_SAVINGS_BALANCE", "50000", "Minimum balance required in savings accounts", "DECIMAL")
        );

        systemSettingRepository.saveAll(settings);
        log.info("✅ Seeded {} system settings.", settings.size());
    }

    private void initializeAdminUser() {
        try {
            userService.getUserByEmail(adminEmail);
            log.info("ℹ️ Admin user already exists: {}", adminEmail);
        } catch (Exception e) {
            log.info("👤 Creating default admin user...");
            userService.createBootstrapUser(
                    "System", "Administrator", adminEmail,
                    generateOfficialEmail(User.Role.ADMIN), adminPhone, adminPassword, User.Role.ADMIN
            );
            log.info("✅ Default admin user created.");
        }
    }

    private String generateOfficialEmail(User.Role role) {
        return role.toString().toLowerCase().replace("_", "") + "@" + officialEmailDomain;
    }

    private SystemSetting createSetting(String key, String value, String desc, String type) {
        return SystemSetting.builder().key(key).value(value).description(desc).dataType(type).build();
    }
}