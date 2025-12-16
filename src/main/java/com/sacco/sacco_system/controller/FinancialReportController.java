package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.entity.FinancialReport;
import com.sacco.sacco_system.service.FinancialReportService;
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
            FinancialReport report = financialReportService.getTodayReport();
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

    // ✅ UPDATED: Accepts 'days' parameter
    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> getChartData(@RequestParam(defaultValue = "7") int days) {
        try {
            List<FinancialReport> reports = financialReportService.getChartData(days);

            // Transform for Frontend
            List<Map<String, Object>> chartData = reports.stream().map(r -> {
                Map<String, Object> point = new HashMap<>();
                // Short Date format (e.g., "12 Dec")
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