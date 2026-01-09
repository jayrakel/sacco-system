package com.sacco.sacco_system.modules.reporting.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.reporting.api.dto.LoanAgingDTO;
import com.sacco.sacco_system.modules.reporting.api.dto.MemberStatementDTO;
import com.sacco.sacco_system.modules.reporting.domain.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final MemberRepository memberRepository;

    // ✅ FIXED: Changed path to '/dashboard-stats' to avoid conflict with FinancialReportController
    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard stats fetched", reportingService.getDashboardStats()));
    }

    @GetMapping("/member-statement/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberStatement(
            @PathVariable UUID memberId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        MemberStatementDTO statement = reportingService.getMemberStatement(memberId, startDate, endDate);
        return ResponseEntity.ok(Map.of("success", true, "data", statement));
    }

    @GetMapping("/my-statement")
    public ResponseEntity<Map<String, Object>> getMyStatement(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member profile not found for this account"));

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        MemberStatementDTO statement = reportingService.getMemberStatement(member.getId(), startDate, endDate);

        return ResponseEntity.ok(Map.of("success", true, "data", statement));
    }

    @GetMapping("/loan-aging")
    public ResponseEntity<Map<String, Object>> getLoanAgingReport() {
        List<LoanAgingDTO> aging = reportingService.getLoanAgingReport();
        return ResponseEntity.ok(Map.of("success", true, "data", aging));
    }
}