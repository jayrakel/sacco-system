package com.sacco.sacco_system.modules.savings.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsWeeklyProcessor {

    private final SavingsService savingsService;

    /**
     * ✅ CRON: Runs every Monday at 01:00 AM
     * "0 0 1 * * MON" = At 01:00:00am, on every Monday
     */
    @Scheduled(cron = "0 0 1 * * MON")
    public void processWeeklyInterest() {
        log.info("📅 Starting Weekly Savings Interest Calculation...");
        try {
            // Call the weekly logic method in SavingsService
            savingsService.applyWeeklyInterest();
            log.info("✅ Weekly Interest Applied Successfully.");
        } catch (Exception e) {
            log.error("❌ Failed to apply weekly interest", e);
        }
    }
}