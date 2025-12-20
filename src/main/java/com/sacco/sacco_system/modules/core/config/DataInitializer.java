package com.sacco.sacco_system.modules.core.config;

import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final AccountingService accountingService;
    private final LoanProductRepository loanProductRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email}")
    private String adminEmail;

    @Value("${app.default-admin.password}")
    private String adminPassword;

    @Value("${app.default-admin.phone}")
    private String adminPhone;

    @Bean
    @Transactional
    public CommandLineRunner initializeData() {
        return args -> {
            log.info("🚀 Starting system initialization...");

            try {
                // 1. Initialize Default Admin User
                initializeAdminUser();

                // 2. Initialize GL Accounts and Mappings
                accountingService.initChartOfAccounts();
                accountingService.initDefaultMappings();

                // 3. Initialize Default Loan Products
                initializeLoanProducts();

                log.info("✅ System initialization completed successfully!");
            } catch (Exception e) {
                log.error("❌ System initialization failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to initialize system", e);
            }
        };
    }

    private void initializeAdminUser() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Creating default admin user...");

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("System")
                    .lastName("Administrator")
                    .phoneNumber(adminPhone)
                    .role(User.Role.ADMIN)
                    .enabled(true)
                    .emailVerified(true)
                    .mustChangePassword(false)
                    .build();

            userRepository.save(admin);
            log.info("✅ Default admin user created: {}", adminEmail);
        } else {
            log.info("ℹ️ Admin user already exists, skipping...");
        }
    }


    private void initializeLoanProducts() {
        long productCount = loanProductRepository.count();

        if (productCount > 0) {
            log.info("ℹ️ Loan products already initialized ({} products), skipping...", productCount);
            return;
        }

        log.info("Creating default loan products...");

        // Normal Loan Product
        LoanProduct normalLoan = LoanProduct.builder()
                .name("Normal Loan")
                .description("Standard loan product with regular terms and conditions. Requires guarantors and has a standard repayment schedule.")
                .interestRate(BigDecimal.valueOf(10.0)) // 10% interest
                .interestType(LoanProduct.InterestType.REDUCING_BALANCE)
                .maxLimit(BigDecimal.valueOf(500000))
                .maxTenureMonths(24) // 2 years max
                .processingFee(BigDecimal.valueOf(500))
                .penaltyRate(BigDecimal.valueOf(2.0)) // 2% penalty
                .build();

        loanProductRepository.save(normalLoan);

        // Emergency Loan Product
        LoanProduct emergencyLoan = LoanProduct.builder()
                .name("Emergency Loan")
                .description("Quick disbursement loan for urgent financial needs. Lower maximum amount but faster processing with minimal requirements.")
                .interestRate(BigDecimal.valueOf(5.0)) // 5% interest (lower for emergencies)
                .interestType(LoanProduct.InterestType.FLAT_RATE)
                .maxLimit(BigDecimal.valueOf(50000))
                .maxTenureMonths(6) // 6 months max
                .processingFee(BigDecimal.valueOf(200))
                .penaltyRate(BigDecimal.valueOf(2.5)) // 2.5% penalty
                .build();

        loanProductRepository.save(emergencyLoan);

        // Development Loan Product
        LoanProduct developmentLoan = LoanProduct.builder()
                .name("Development Loan")
                .description("Long-term loan for business development, property investment, or major purchases. Higher limits with extended repayment period.")
                .interestRate(BigDecimal.valueOf(12.0)) // 12% interest
                .interestType(LoanProduct.InterestType.REDUCING_BALANCE)
                .maxLimit(BigDecimal.valueOf(1000000))
                .maxTenureMonths(36) // 3 years max
                .processingFee(BigDecimal.valueOf(1000))
                .penaltyRate(BigDecimal.valueOf(2.0)) // 2% penalty
                .build();

        loanProductRepository.save(developmentLoan);

        log.info("✅ Created 3 default loan products: Normal, Emergency, and Development loans");
    }
}

