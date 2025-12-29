package com.sacco.sacco_system.modules.analytics.api.controller.handler;

import com.sacco.sacco_system.modules.analytics.domain.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnalyticsReadHandler {

    // ✅ Correctly importing the refactored Service Facade
    private final AnalyticsService analyticsService;

    public ResponseEntity<?> getMemberGrowth() {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getMemberGrowthAnalytics()));
    }

    public ResponseEntity<?> getLoanPortfolio() {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getLoanPortfolioAnalytics()));
    }

    public ResponseEntity<?> getSavingsAnalytics() {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getSavingsAnalytics()));
    }

    public ResponseEntity<?> getPerformanceTrends(int months) {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getPerformanceTrends(months)));
    }

    public ResponseEntity<?> getTopPerformers(int limit) {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getTopPerformers(limit)));
    }

    public ResponseEntity<?> getDashboardStatistics() {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getDashboardStatistics()));
    }

    public ResponseEntity<?> getFinancialHealthScore() {
        return ResponseEntity.ok(Map.of("success", true, "data", analyticsService.getFinancialHealthScore()));
    }
}