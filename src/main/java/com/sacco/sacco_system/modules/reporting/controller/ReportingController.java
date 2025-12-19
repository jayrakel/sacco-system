package com.sacco.sacco_system.modules.reporting.controller;

import com.sacco.sacco_system.modules.loans.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.members.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/member-statement/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberStatement(@PathVariable UUID memberId) {
        List<MemberStatementDTO> statement = reportingService.getMemberStatement(memberId);
        return ResponseEntity.ok(Map.of("success", true, "data", statement));
    }

    @GetMapping("/loan-aging")
    public ResponseEntity<Map<String, Object>> getLoanAgingReport() {
        List<LoanAgingDTO> aging = reportingService.getLoanAgingReport();
        return ResponseEntity.ok(Map.of("success", true, "data", aging));
    }
}