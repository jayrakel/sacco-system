package com.sacco.sacco_system.modules.analytics.api.controller;

import com.sacco.sacco_system.modules.analytics.api.controller.handler.AnalyticsReadHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsReadHandler readHandler;

    @GetMapping("/member-growth")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getMemberGrowth() {
        return readHandler.getMemberGrowth();
    }

    @GetMapping("/loan-portfolio")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getLoanPortfolio() {
        return readHandler.getLoanPortfolio();
    }

    @GetMapping("/savings")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getSavingsAnalytics() {
        return readHandler.getSavingsAnalytics();
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getPerformanceTrends(@RequestParam(defaultValue = "12") int months) {
        return readHandler.getPerformanceTrends(months);
    }

    @GetMapping("/top-performers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getTopPerformers(@RequestParam(defaultValue = "10") int limit) {
        return readHandler.getTopPerformers(limit);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getDashboardStatistics() {
        return readHandler.getDashboardStatistics();
    }

    @GetMapping("/health-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getFinancialHealthScore() {
        return readHandler.getFinancialHealthScore();
    }
}