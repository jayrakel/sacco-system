package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.service.AccountingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Accounting Reports Controller
 * Provides financial statements from GL accounts and journal entries
 */
@RestController
@RequestMapping("/api/accounting/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountingReportController {

    private final AccountingReportService reportService;

    /**
     * Get Balance Sheet as of a specific date
     * Shows Assets, Liabilities, and Equity
     */
    @GetMapping("/balance-sheet")
    public ResponseEntity<Map<String, Object>> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();
        Map<String, Object> balanceSheet = reportService.getBalanceSheet(reportDate);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", balanceSheet
        ));
    }

    /**
     * Get Income Statement for a date range
     * Shows Revenue, Expenses, and Net Income
     */
    @GetMapping("/income-statement")
    public ResponseEntity<Map<String, Object>> getIncomeStatement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Map<String, Object> incomeStatement = reportService.getIncomeStatement(start, end);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", incomeStatement
        ));
    }

    /**
     * Get Trial Balance as of a specific date
     * Shows all account balances with debits and credits
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<Map<String, Object>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();
        Map<String, Object> trialBalance = reportService.getTrialBalance(reportDate);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", trialBalance
        ));
    }

    /**
     * Get Account Activity for a date range
     * Shows transaction volume per account
     */
    @GetMapping("/account-activity")
    public ResponseEntity<Map<String, Object>> getAccountActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Map<String, Object> activity = reportService.getAccountActivity(start, end);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", activity
        ));
    }

    /**
     * Get Cash Flow Summary for a date range
     * Shows cash inflows, outflows, and net cash flow
     */
    @GetMapping("/cash-flow")
    public ResponseEntity<Map<String, Object>> getCashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        Map<String, Object> cashFlow = reportService.getCashFlowSummary(start, end);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", cashFlow
        ));
    }

    /**
     * Get all reports at once (for dashboard)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardReports() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        Map<String, Object> dashboard = Map.of(
                "balanceSheet", reportService.getBalanceSheet(today),
                "incomeStatement", reportService.getIncomeStatement(monthStart, today),
                "cashFlow", reportService.getCashFlowSummary(monthStart, today)
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dashboard
        ));
    }
}

