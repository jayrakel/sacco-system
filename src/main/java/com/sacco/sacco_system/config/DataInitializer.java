package com.sacco.sacco_system.config;

import com.sacco.sacco_system.entity.SystemSetting;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.SystemSettingRepository; // ✅ Import
import com.sacco.sacco_system.repository.UserRepository;
import com.sacco.sacco_system.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountingService accountingService;
    private final SystemSettingRepository systemSettingRepository; // ✅ Inject Repository

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // 1. Create Admin
        createDefaultAdminIfNotExist();

        // 2. Initialize Settings (Fees & Bank Details)
        initDefaultSettings();

        // 3. Initialize Finance System
        try {
            accountingService.initChartOfAccounts();

            // ✅ ADD THIS LINE: Ensure mappings are always up to date
            accountingService.initDefaultMappings();

            System.out.println("✅ Chart of Accounts & Mappings checked/initialized.");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize Chart of Accounts: " + e.getMessage());
        }
    }

    private void createDefaultAdminIfNotExist() {
        Optional<User> adminExists = userRepository.findByEmail(adminEmail);
        if (adminExists.isEmpty()) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .mustChangePassword(true)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Default Admin created successfully!");
        } else {
            System.out.println("ℹ️ Admin account already exists.");
        }
    }

    // ✅ NEW: Seed Default Settings
    private void initDefaultSettings() {
        // Financials
        createSetting("REGISTRATION_FEE", "1000", "Fee paid by new members");
        createSetting("MIN_SHARE_CAPITAL", "5000", "Minimum shares to hold");
        createSetting("LOAN_INTEREST_RATE", "12", "Annual Interest Rate (%)");

        // Bank / Deposit Details
        createSetting("BANK_NAME", "Equity Bank", "Main Sacco Bank Name");
        createSetting("BANK_ACCOUNT_NO", "0000000000", "Sacco Bank Account Number");
        createSetting("PAYBILL_NO", "000000", "M-Pesa Paybill Number");

        System.out.println("✅ System Settings checked/initialized.");
    }

    private void createSetting(String key, String value, String desc) {
        if (systemSettingRepository.findByKey(key).isEmpty()) {
            SystemSetting setting = SystemSetting.builder()
                    .key(key)
                    .value(value)
                    .description(desc)
                    .build();
            systemSettingRepository.save(setting);
        }
    }
}