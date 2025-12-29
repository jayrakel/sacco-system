package com.sacco.sacco_system.modules.auth.service.setup.impl;

import com.sacco.sacco_system.modules.admin.domain.entity.SystemSetting;
import com.sacco.sacco_system.modules.admin.domain.repository.SystemSettingRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetupSystemWorker {

    private final AccountingService accountingService;
    private final SystemSettingRepository systemSettingRepository;

    public void initializeChartOfAccounts() {
        try {
            accountingService.initChartOfAccounts();
        } catch (Exception e) {
            log.error("Failed to initialize Chart of Accounts: {}", e.getMessage());
            throw new RuntimeException("Accounting setup failed: " + e.getMessage());
        }
    }

    public void markSetupAsComplete() {
        try {
            if (systemSettingRepository.findByKey("SETUP_COMPLETE").isPresent()) {
                return;
            }

            SystemSetting setupComplete = SystemSetting.builder()
                    .key("SETUP_COMPLETE")
                    .value("true")
                    .description("Flag to indicate system setup is done")
                    .dataType("BOOLEAN")
                    .build();
            systemSettingRepository.save(setupComplete);
        } catch (Exception e) {
            log.warn("Failed to save setup complete flag: {}", e.getMessage());
        }
    }
}