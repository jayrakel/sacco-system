package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.FinancialReport;
import com.sacco.sacco_system.modules.finance.domain.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport() {
        try {
            FinancialReport report = financialReportService.generateDailyReport();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);
            response.put("message", "Report generated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayReport() {
        try {
            System.out.println("📊 [FinancialReportController] Fetching today's report...");
            FinancialReport report = financialReportService.getTodayReport();

            System.out.println("✅ [FinancialReportController] Report generated successfully");
            System.out.println("   - Report Date: " + report.getReportDate());
            System.out.println("   - Total Members: " + report.getTotalMembers());
            System.out.println("   - Total Savings: " + report.getTotalSavings());
            System.out.println("   - Total Loans: " + report.getTotalLoansIssued());
            System.out.println("   - Net Income: " + report.getNetIncome());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ [FinancialReportController] Error getting today's report:");
            System.err.println("   Error Type: " + e.getClass().getName());
            System.err.println("   Error Message: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error generating report: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<Map<String, Object>> getReportByDate(@PathVariable String date) {
        try {
            LocalDate reportDate = LocalDate.parse(date);
            FinancialReport report = financialReportService.getReportByDate(reportDate);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // âœ… UPDATED: Supports Custom Date Range OR 'days' fallback
    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> getChartData(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "7") int days) {
        try {
            List<FinancialReport> reports;

            // 1. Check if specific dates are provided
            if (startDate != null && endDate != null) {
                reports = financialReportService.getChartDataCustom(startDate, endDate);
            } else {
                // 2. Fallback to 'last N days' logic
                reports = financialReportService.getChartData(days);
            }

            // Transform for Frontend (Recharts format)
            List<Map<String, Object>> chartData = reports.stream().map(r -> {
                Map<String, Object> point = new HashMap<>();
                // Format: "12 Dec"
                point.put("name", r.getReportDate().getDayOfMonth() + " " + r.getReportDate().getMonth().name().substring(0, 3));
                point.put("income", r.getTotalIncome());
                point.put("expense", r.getTotalExpenses());
                return point;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chartData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}





