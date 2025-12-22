package com.sacco.sacco_system.config;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.AccountType;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import com.sacco.sacco_system.modules.finance.domain.repository.GLAccountRepository;
import com.sacco.sacco_system.modules.finance.domain.service.ChartOfAccountsSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final ChartOfAccountsSetupService setupService;
    private final GLAccountRepository accountRepository;

    @Override
    public void run(String... args) {
        try {
            log.info("🔧 Checking database initialization...");
            
            if (!setupService.isInitialized()) {
                log.info("📊 Initializing Chart of Accounts and GL Mappings...");
                setupService.initializeChartOfAccounts();
                setupService.initializeGLMappings();
                log.info("✅ Database initialized successfully");
            } else {
                log.info("✅ Chart of Accounts already initialized");
                
                // Check and add missing critical accounts
                ensureCriticalAccountsExist();
                
                // Always run GL mappings to add any missing ones
                setupService.initializeGLMappings();
            }
        } catch (Exception e) {
            log.error("❌ Error during database initialization: {}", e.getMessage(), e);
        }
    }
    
    private void ensureCriticalAccountsExist() {
        // Check if Share Capital account exists
        if (accountRepository.findByCode("2020").isEmpty()) {
            log.info("📝 Adding missing Share Capital account (2020)...");
            GLAccount shareCapital = GLAccount.builder()
                    .code("2020")
                    .name("Share Capital")
                    .type(AccountType.LIABILITY)
                    .active(true)
                    .build();
            accountRepository.save(shareCapital);
            log.info("✅ Share Capital account added");
        }
        
        // Check if Member Savings account exists
        if (accountRepository.findByCode("2010").isEmpty()) {
            log.info("📝 Adding missing Member Savings Deposits account (2010)...");
            GLAccount savings = GLAccount.builder()
                    .code("2010")
                    .name("Member Savings Deposits")
                    .type(AccountType.LIABILITY)
                    .active(true)
                    .build();
            accountRepository.save(savings);
            log.info("✅ Member Savings Deposits account added");
        }
    }
}
