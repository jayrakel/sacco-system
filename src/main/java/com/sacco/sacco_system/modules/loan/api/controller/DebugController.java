package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final MemberRepository memberRepository;
    private final GuarantorRepository guarantorRepository;
    private final SystemSettingService systemSettingService;

    /**
     * Debug endpoint to check guarantor capacity calculation
     */
    @GetMapping("/guarantor/{memberId}")
    public ResponseEntity<Map<String, Object>> debugGuarantorCapacity(@PathVariable UUID memberId) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> debug = new HashMap<>();

            // Basic member info
            debug.put("memberNumber", member.getMemberNumber());
            debug.put("memberName", member.getFirstName() + " " + member.getLastName());
            debug.put("memberStatus", member.getStatus());

            // Savings info
            BigDecimal currentSavings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
            debug.put("totalSavings", currentSavings);

            // System settings
            double maxGuarantorRatio = systemSettingService.getDouble("MAX_GUARANTOR_LIMIT_RATIO");
            debug.put("maxGuarantorRatio", maxGuarantorRatio);

            // Calculate max capacity
            BigDecimal maxGuarantorLimit = currentSavings.multiply(BigDecimal.valueOf(maxGuarantorRatio));
            debug.put("maxGuarantorLimit", maxGuarantorLimit);

            // Get ALL guarantor commitments (all statuses)
            List<Guarantor> allGuarantees = guarantorRepository.findByMemberId(member.getId());
            debug.put("totalGuaranteeRecords", allGuarantees.size());

            // Break down by status
            Map<Guarantor.GuarantorStatus, List<Guarantor>> byStatus = allGuarantees.stream()
                    .collect(Collectors.groupingBy(Guarantor::getStatus));

            Map<String, Object> statusBreakdown = new HashMap<>();
            for (Guarantor.GuarantorStatus status : Guarantor.GuarantorStatus.values()) {
                List<Guarantor> guarantees = byStatus.getOrDefault(status, Collections.emptyList());
                BigDecimal total = guarantees.stream()
                        .map(Guarantor::getGuaranteeAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                statusBreakdown.put(status.toString(), Map.of(
                    "count", guarantees.size(),
                    "totalAmount", total,
                    "details", guarantees.stream().map(g -> Map.of(
                        "loanId", g.getLoan().getId(),
                        "amount", g.getGuaranteeAmount(),
                        "status", g.getStatus()
                    )).collect(Collectors.toList())
                ));
            }
            debug.put("guaranteesByStatus", statusBreakdown);

            // Calculate current exposure (ACCEPTED only - current logic)
            BigDecimal acceptedExposure = guarantorRepository.findByMemberIdAndStatus(
                member.getId(),
                Guarantor.GuarantorStatus.ACCEPTED
            ).stream()
                .map(Guarantor::getGuaranteeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            debug.put("currentExposure_ACCEPTED_ONLY", acceptedExposure);

            // Calculate with PENDING included (recommended)
            BigDecimal acceptedAndPendingExposure = allGuarantees.stream()
                    .filter(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED ||
                                 g.getStatus() == Guarantor.GuarantorStatus.PENDING)
                    .map(Guarantor::getGuaranteeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            debug.put("currentExposure_ACCEPTED_AND_PENDING", acceptedAndPendingExposure);

            // Available capacity (current logic)
            BigDecimal availableWithCurrentLogic = maxGuarantorLimit.subtract(acceptedExposure);
            debug.put("availableToGuarantee_CURRENT_LOGIC", availableWithCurrentLogic);

            // Available capacity (recommended logic)
            BigDecimal availableRecommended = maxGuarantorLimit.subtract(acceptedAndPendingExposure);
            debug.put("availableToGuarantee_RECOMMENDED", availableRecommended);

            // Analysis
            debug.put("analysis", Map.of(
                "isOverCommitted", availableWithCurrentLogic.compareTo(BigDecimal.ZERO) < 0,
                "wouldBeOverCommittedWithPending", availableRecommended.compareTo(BigDecimal.ZERO) < 0,
                "message", availableWithCurrentLogic.compareTo(BigDecimal.ZERO) < 0 ?
                    "WARNING: Member is over-committed!" : "Member has capacity available"
            ));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", debug
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}

