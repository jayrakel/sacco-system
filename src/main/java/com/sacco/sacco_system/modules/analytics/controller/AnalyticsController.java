package com.sacco.sacco_system.modules.analytics.controller;

import com.sacco.sacco_system.modules.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Analytics Controller
 * Provides advanced analytics and insights
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get member growth analytics
     */
    @GetMapping("/member-growth")
    public ResponseEntity<Map<String, Object>> getMemberGrowth() {
        Map<String, Object> analytics = analyticsService.getMemberGrowthAnalytics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analytics
        ));
    }

    /**
     * Get loan portfolio analytics
     */
    @GetMapping("/loan-portfolio")
    public ResponseEntity<Map<String, Object>> getLoanPortfolio() {
        Map<String, Object> analytics = analyticsService.getLoanPortfolioAnalytics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analytics
        ));
    }

    /**
     * Get savings analytics
     */
    @GetMapping("/savings")
    public ResponseEntity<Map<String, Object>> getSavingsAnalytics() {
        Map<String, Object> analytics = analyticsService.getSavingsAnalytics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analytics
        ));
    }

    /**
     * Get performance trends
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getPerformanceTrends(
            @RequestParam(defaultValue = "12") int months) {
        Map<String, Object> trends = analyticsService.getPerformanceTrends(months);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", trends
        ));
    }

    /**
     * Get top performers
     */
    @GetMapping("/top-performers")
    public ResponseEntity<Map<String, Object>> getTopPerformers(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> performers = analyticsService.getTopPerformers(limit);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", performers
        ));
    }

    /**
     * Get comprehensive dashboard statistics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> stats = analyticsService.getDashboardStatistics();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
        ));
    }

    /**
     * Get financial health score
     */
    @GetMapping("/health-score")
    public ResponseEntity<Map<String, Object>> getFinancialHealthScore() {
        Map<String, Object> healthScore = analyticsService.getFinancialHealthScore();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", healthScore
        ));
    }
}

