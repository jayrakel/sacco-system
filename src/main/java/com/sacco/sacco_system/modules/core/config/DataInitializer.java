package com.sacco.sacco_system.modules.core.config;

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsProduct;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsProductRepository;
import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
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

    private final UserRepository userRepository;
    private final AccountingService accountingService;
    // LoanProductRepository removed ✅
    private final SavingsProductRepository savingsProductRepository;
    private final SystemSettingRepository systemSettingRepository;
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
                // 1. Initialize System Settings (The "Betterlink" Rules)
                initializeSystemSettings();

                // 2. Initialize Default Admin User
                initializeAdminUser();

                // 3. Initialize GL Accounts and Mappings
                accountingService.initChartOfAccounts();
                accountingService.initDefaultMappings();

                // 4. Initialize Savings Products
                initializeSavingsProducts();

                // 5. Loan Products Initialization REMOVED as requested ✅

                log.info("✅ System initialization completed successfully!");
            } catch (Exception e) {
                log.error("❌ System initialization failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize system", e);
            }
        };
    }

    private void initializeSystemSettings() {
        if (systemSettingRepository.count() > 0) {
            log.info("ℹ️ System settings already initialized, skipping...");
            return;
        }

        log.info("⚙️ Seeding default system settings...");

        List<SystemSetting> settings = Arrays.asList(
                createSetting("sacco_name", "Fresh Start Sacco", "Organization Name", "STRING"),
                createSetting("registration_fee", "1500", "One-time mandatory fee (KES)", "DECIMAL"),
                createSetting("min_share_capital", "2000", "Minimum shares to fully join (KES)", "DECIMAL"),
                createSetting("min_weekly_deposit", "250", "Minimum saving per week/month (KES)", "DECIMAL"),
                createSetting("penalty_missed_savings", "50", "Penalty for missed contribution", "DECIMAL"),
                createSetting("min_savings_for_loan", "5000", "Min savings required to apply for a loan", "DECIMAL"),
                createSetting("loan_interest_rate", "12", "Default Annual interest rate (%)", "DECIMAL"),
                createSetting("loan_multiplier", "3", "Loan limit multiplier (x Savings)", "INTEGER"),
                createSetting("loan_processing_fee", "500", "Standard processing fee (KES)", "DECIMAL"),
                createSetting("min_guarantors", "2", "Minimum guarantors required", "INTEGER")
        );

        systemSettingRepository.saveAll(settings);
        log.info("✅ Seeded {} system settings.", settings.size());
    }

    private void initializeAdminUser() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("👤 Creating default admin user...");
            log.info("📧 Admin Email: {}", adminEmail);
            log.info("🔑 Admin Password (raw): {}", adminPassword);
            log.info("📞 Admin Phone: {}", adminPhone);

            String encodedPassword = passwordEncoder.encode(adminPassword);
            log.info("🔐 Encoded Password: {}", encodedPassword);
            log.info("✅ Password matches test: {}", passwordEncoder.matches(adminPassword, encodedPassword));

            User admin = User.builder()
                    .email(adminEmail)
                    .officialEmail(generateOfficialEmail(User.Role.ADMIN))
                    .password(encodedPassword)
                    .firstName("System")
                    .lastName("Administrator")
                    .phoneNumber(adminPhone)
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .emailVerified(true)
                    .mustChangePassword(true)
                    .build();

            userRepository.save(admin);
            log.info("✅ Default admin user created: {} with official email: {}", adminEmail, admin.getOfficialEmail());
        } else {
            log.info("ℹ️ Admin user already exists: {}", adminEmail);
        }
    }

    private String generateOfficialEmail(User.Role role) {
        return role.toString().toLowerCase().replace("_", "") + "@" + officialEmailDomain;
    }

    private void initializeSavingsProducts() {
        if (savingsProductRepository.count() > 0) {
            log.info("ℹ️ Savings products already initialized, skipping...");
            return;
        }

        log.info("💰 Creating default Savings Products...");

        SavingsProduct shareCapital = SavingsProduct.builder()
                .name("Share Capital")
                .description("Non-withdrawable ownership shares. Required for membership.")
                .type(SavingsProduct.ProductType.SAVINGS)
                .interestRate(BigDecimal.valueOf(10.0))
                .minBalance(BigDecimal.valueOf(2000))
                .allowWithdrawal(false)
                .build();

        SavingsProduct ordinaryDeposit = SavingsProduct.builder()
                .name("Ordinary Deposit")
                .description("Regular monthly savings used as collateral for loans.")
                .type(SavingsProduct.ProductType.SAVINGS)
                .interestRate(BigDecimal.valueOf(5.0))
                .minBalance(BigDecimal.ZERO)
                .allowWithdrawal(false)
                .build();

        SavingsProduct holidayAccount = SavingsProduct.builder()
                .name("Holiday Account")
                .description("Optional savings for holidays. Withdrawable after 12 months.")
                .type(SavingsProduct.ProductType.SAVINGS)
                .interestRate(BigDecimal.valueOf(3.0))
                .minDurationMonths(12)
                .allowWithdrawal(true)
                .build();

        savingsProductRepository.saveAll(Arrays.asList(shareCapital, ordinaryDeposit, holidayAccount));
        log.info("✅ Created 3 Savings Products: Share Capital, Ordinary Deposit, Holiday Account.");
    }

    private SystemSetting createSetting(String key, String value, String desc, String type) {
        return SystemSetting.builder()
                .key(key)
                .value(value)
                .description(desc)
                .dataType(type)
                .build();
    }
}