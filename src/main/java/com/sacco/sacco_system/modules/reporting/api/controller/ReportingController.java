package com.sacco.sacco_system.modules.reporting.api.controller;

import com.sacco.sacco_system.modules.reporting.api.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.reporting.api.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.reporting.domain.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/member-statement/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberStatement(
            @PathVariable UUID memberId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        // Default to last 30 days if not provided
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        MemberStatementDTO statement = reportingService.getMemberStatement(memberId, startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", statement));
    }

    @GetMapping("/loan-aging")
    public ResponseEntity<Map<String, Object>> getLoanAgingReport() {
        List<LoanAgingDTO> aging = reportingService.getLoanAgingReport();
        return ResponseEntity.ok(Map.of("success", true, "data", aging));
    }
}