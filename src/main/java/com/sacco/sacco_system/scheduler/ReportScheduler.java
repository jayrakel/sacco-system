package com.sacco.sacco_system.scheduler;

import com.sacco.sacco_system.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private final FinancialReportService financialReportService;

    // Run every day at 23:59 (11:59 PM)
    @Scheduled(cron = "0 59 23 * * *")
    public void runDailyReport() {
        log.info("⏰ Starting scheduled Daily Financial Report generation...");
        try {
            financialReportService.generateDailyReport();
            log.info("✅ Daily Financial Report generated successfully.");
        } catch (Exception e) {
            log.error("❌ Failed to generate daily report: {}", e.getMessage());
        }
    }
}