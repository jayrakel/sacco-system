package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Fine;
import com.sacco.sacco_system.modules.finance.domain.repository.FineRepository;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final LoanRepaymentRepository loanRepaymentRepository;
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
            // Link to loan if provided
            fine.setLoan(member.getLoans().stream()
                    .filter(l -> l.getId().equals(loanId))
                    .findFirst()
                    .orElse(null));
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
        LoanRepayment repayment = loanRepaymentRepository.findById(loanRepaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));

        if (repayment.getStatus() == LoanRepayment.RepaymentStatus.PAID) {
            throw new RuntimeException("Repayment already paid. No fine needed.");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = repayment.getDueDate();

        if (today.isBefore(dueDate) || today.isEqual(dueDate)) {
            throw new RuntimeException("Payment not yet overdue. No fine applicable.");
        }

        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);

        // Get fine rate from settings (e.g., 1% per day or fixed amount)
        BigDecimal fineRate = BigDecimal.valueOf(
                systemSettingService.getDouble("LATE_PAYMENT_FINE_RATE", 0.01)); // 1% per day default

        // Calculate fine amount
        BigDecimal fineAmount = repayment.getAmount()
                .multiply(fineRate)
                .multiply(BigDecimal.valueOf(daysOverdue));

        // Check for maximum fine cap
        BigDecimal maxFine = BigDecimal.valueOf(
                systemSettingService.getDouble("MAX_LATE_PAYMENT_FINE", 5000.0));

        if (fineAmount.compareTo(maxFine) > 0) {
            fineAmount = maxFine;
        }

        String description = String.format("Late payment fine for %d days overdue on installment #%d",
                daysOverdue, repayment.getRepaymentNumber());

        return imposeFine(
                repayment.getLoan().getMember().getId(),
                repayment.getLoan().getId(),
                Fine.FineType.LATE_LOAN_PAYMENT,
                fineAmount,
                description,
                (int) daysOverdue
        );
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
        List<LoanRepayment> overduePayments = loanRepaymentRepository
                .findByStatusAndDueDateBefore(
                        LoanRepayment.RepaymentStatus.PENDING,
                        LocalDate.now()
                );

        List<Fine> finesImposed = new java.util.ArrayList<>();

        for (LoanRepayment repayment : overduePayments) {
            try {
                // Check if fine already exists for this repayment
                List<Fine> existingFines = fineRepository.findByLoanId(repayment.getLoan().getId());
                boolean fineExists = existingFines.stream()
                        .anyMatch(f -> f.getDescription().contains("installment #" + repayment.getRepaymentNumber()));

                if (!fineExists) {
                    Fine fine = calculateLateFine(repayment.getId());
                    finesImposed.add(fine);
                }
            } catch (Exception e) {
                log.warn("Failed to impose fine for repayment {}: {}",
                        repayment.getId(), e.getMessage());
            }
        }

        log.info("Processed {} overdue payments, imposed {} fines",
                overduePayments.size(), finesImposed.size());

        return finesImposed;
    }
}

