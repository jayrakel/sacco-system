package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import com.sacco.sacco_system.modules.finance.domain.repository.FineRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fine Service
 * Manages fines and penalties for late payments and other infractions
 * Integrated with accounting system
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FineService {

    private final FineRepository fineRepository;
    private final MemberRepository memberRepository;
    private final SystemSettingService systemSettingService;
    private final AccountingService accountingService;

    /**
     * Impose a fine on a member
     */
    public Fine imposeFine(UUID memberId, UUID loanId, Fine.FineType type,
                          BigDecimal amount, String description, Integer daysOverdue) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Fine fine = Fine.builder()
                .member(member)
                .type(type)
                .amount(amount)
                .description(description)
                .fineDate(LocalDate.now())
                .status(Fine.FineStatus.PENDING)
                .daysOverdue(daysOverdue)
                .build();

        if (loanId != null) {
            // Loans module removed: cannot link fine to loan. Persist fine without loan link.
            log.warn("Loan ID provided to imposeFine but loans module is removed: {}", loanId);
        }

        Fine saved = fineRepository.save(fine);

        log.info("Fine imposed on member {}: KES {} for {}",
                member.getMemberNumber(), amount, type);

        return saved;
    }

    /**
     * Calculate and impose late payment fine for overdue loan repayment
     */
    public Fine calculateLateFine(UUID loanRepaymentId) {
        // Late fine calculation depends on LoanRepayment data which is part of the removed loans module.
        throw new UnsupportedOperationException("Loan repayment fine calculation is unavailable because the loans module has been removed.");
    }

    /**
     * Pay a fine
     */
    public Fine payFine(UUID fineId, String paymentReference) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Fine not found"));

        if (fine.getStatus() == Fine.FineStatus.PAID) {
            throw new RuntimeException("Fine already paid");
        }

        if (fine.getStatus() == Fine.FineStatus.WAIVED) {
            throw new RuntimeException("Fine was waived");
        }

        fine.setStatus(Fine.FineStatus.PAID);
        fine.setPaymentDate(LocalDate.now());
        fine.setPaymentReference(paymentReference);

        Fine saved = fineRepository.save(fine);

        // ✅ POST TO ACCOUNTING
        accountingService.postFinePayment(fine.getMember(), fine.getAmount());
        // Creates: DEBIT Cash (1020), CREDIT Other Income (4040)

        log.info("Fine paid by member {}: KES {}",
                fine.getMember().getMemberNumber(), fine.getAmount());

        return saved;
    }

    /**
     * Waive a fine (forgive)
     */
    public Fine waiveFine(UUID fineId, String reason) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Fine not found"));

        if (fine.getStatus() == Fine.FineStatus.PAID) {
            throw new RuntimeException("Cannot waive paid fine");
        }

        fine.setStatus(Fine.FineStatus.WAIVED);
        fine.setDescription(fine.getDescription() + " | Waived: " + reason);

        Fine saved = fineRepository.save(fine);

        log.info("Fine waived for member {}: KES {} - Reason: {}",
                fine.getMember().getMemberNumber(), fine.getAmount(), reason);

        return saved;
    }

    /**
     * Get member's fines
     */
    public List<Fine> getMemberFines(UUID memberId) {
        return fineRepository.findByMemberId(memberId);
    }

    /**
     * Get member's pending fines
     */
    public List<Fine> getMemberPendingFines(UUID memberId) {
        return fineRepository.findByMemberIdAndStatus(memberId, Fine.FineStatus.PENDING);
    }

    /**
     * Get total pending fines for a member
     */
    public BigDecimal getMemberPendingFinesTotal(UUID memberId) {
        BigDecimal total = fineRepository.getTotalPendingFinesByMember(memberId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get all pending fines (for admin)
     */
    public List<Fine> getAllPendingFines() {
        return fineRepository.findByStatus(Fine.FineStatus.PENDING);
    }

    /**
     * Get fine statistics
     */
    public Map<String, Object> getFineStatistics() {
        BigDecimal totalPaid = fineRepository.getTotalFinesPaid();
        BigDecimal totalPending = fineRepository.getTotalPendingFines();

        List<Fine> allFines = fineRepository.findAll();
        long paid = allFines.stream().filter(f -> f.getStatus() == Fine.FineStatus.PAID).count();
        long pending = allFines.stream().filter(f -> f.getStatus() == Fine.FineStatus.PENDING).count();
        long waived = allFines.stream().filter(f -> f.getStatus() == Fine.FineStatus.WAIVED).count();

        return Map.of(
                "totalFines", allFines.size(),
                "paidCount", paid,
                "pendingCount", pending,
                "waivedCount", waived,
                "totalPaidAmount", totalPaid != null ? totalPaid : BigDecimal.ZERO,
                "totalPendingAmount", totalPending != null ? totalPending : BigDecimal.ZERO
        );
    }

    /**
     * Automated: Check for overdue payments and impose fines
     * This can be scheduled to run daily
     */
    public List<Fine> processOverduePayments() {
        throw new UnsupportedOperationException("Overdue payment processing is unavailable because the loans module has been removed.");
    }
}
