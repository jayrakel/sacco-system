package com.sacco.sacco_system.modules.core.config.seeder;

import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class AccountingSeeder {

    private final AccountingService accountingService;

    public void seed() {
        try {
            accountingService.initChartOfAccounts();
            accountingService.initDefaultMappings();
        } catch (Exception e) {
            log.error("Failed to initialize accounting data: {}", e.getMessage());
        }
    }
}