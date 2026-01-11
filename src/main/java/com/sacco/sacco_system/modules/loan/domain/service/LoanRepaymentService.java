package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.core.exception.ApiException;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.service.GuarantorService;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepaymentSchedule;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanRepaymentService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentScheduleRepository scheduleRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final ReferenceCodeService referenceCodeService;
    private final GuarantorService guarantorService;

    /**
     * ✅ PRIMARY LOGIC: Process a Loan Repayment with "Smart Allocation"
     */
    @Transactional
    public void processRepayment(UUID loanId, BigDecimal amount, String paymentMethod, String sourceReference) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ApiException("Loan not found", 404));

        if (loan.getLoanStatus() == Loan.LoanStatus.CLOSED ||
                loan.getLoanStatus() == Loan.LoanStatus.WRITTEN_OFF) {
            throw new ApiException("Cannot repay a closed or written-off loan", 400);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Repayment amount must be greater than zero", 400);
        }

        log.info("Processing repayment of {} for Loan {}", amount, loan.getLoanNumber());

        // 1. Fetch Unpaid Schedules (Oldest First)
        List<LoanRepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        BigDecimal remainingAmount = amount;
        BigDecimal totalPrincipalPaid = BigDecimal.ZERO;
        BigDecimal totalInterestPaid = BigDecimal.ZERO;

        // 2. Waterfall Logic: Distribute amount across installments
        for (LoanRepaymentSchedule schedule : schedules) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;

            if (schedule.getStatus() == LoanRepaymentSchedule.InstallmentStatus.PAID) continue;

            // Calculate what is still owed on this specific installment
            BigDecimal dueOnInstallment = schedule.getTotalDue().subtract(schedule.getPaidAmount());

            BigDecimal paymentForThisSchedule;
            if (remainingAmount.compareTo(dueOnInstallment) >= 0) {
                // Fully pay this installment
                paymentForThisSchedule = dueOnInstallment;
                schedule.setPaidAmount(schedule.getTotalDue());
                schedule.setStatus(LoanRepaymentSchedule.InstallmentStatus.PAID);
            } else {
                // Partially pay this installment
                paymentForThisSchedule = remainingAmount;
                schedule.setPaidAmount(schedule.getPaidAmount().add(paymentForThisSchedule));
                schedule.setStatus(LoanRepaymentSchedule.InstallmentStatus.PARTIALLY_PAID);
            }

            // Estimate Split for Accounting
            BigDecimal principalShare = BigDecimal.ZERO;
            BigDecimal interestShare = BigDecimal.ZERO;

            if (paymentForThisSchedule.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalInstallmentAmt = schedule.getTotalDue();
                if (totalInstallmentAmt.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal principalRatio = schedule.getPrincipalDue().divide(totalInstallmentAmt, 4, BigDecimal.ROUND_HALF_UP);
                    principalShare = paymentForThisSchedule.multiply(principalRatio).setScale(2, BigDecimal.ROUND_HALF_UP);
                    interestShare = paymentForThisSchedule.subtract(principalShare);
                }
            }

            totalPrincipalPaid = totalPrincipalPaid.add(principalShare);
            totalInterestPaid = totalInterestPaid.add(interestShare);

            remainingAmount = remainingAmount.subtract(paymentForThisSchedule);
            scheduleRepository.save(schedule);
        }

        // 3. Handle Overpayment (Reduce Principal)
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            totalPrincipalPaid = totalPrincipalPaid.add(remainingAmount);
        }

        // 4. Update Loan Entity
        BigDecimal newOutstandingPrincipal = loan.getOutstandingPrincipal().subtract(totalPrincipalPaid);
        BigDecimal newOutstandingInterest = loan.getOutstandingInterest().subtract(totalInterestPaid);
        BigDecimal newTotalOutstanding = loan.getTotalOutstandingAmount().subtract(amount);

        // Prevent negatives
        if (newOutstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) newOutstandingPrincipal = BigDecimal.ZERO;
        if (newOutstandingInterest.compareTo(BigDecimal.ZERO) < 0) newOutstandingInterest = BigDecimal.ZERO;
        if (newTotalOutstanding.compareTo(BigDecimal.ZERO) < 0) newTotalOutstanding = BigDecimal.ZERO;

        loan.setOutstandingPrincipal(newOutstandingPrincipal);
        loan.setOutstandingInterest(newOutstandingInterest);
        loan.setTotalOutstandingAmount(newTotalOutstanding);

        // Auto-Close Logic
        if (newTotalOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
            guarantorService.unlockGuarantorFunds(loan); // Unlock funds
            log.info("Loan {} has been fully repaid and CLOSED.", loan.getLoanNumber());
        } else if (loan.getLoanStatus() == Loan.LoanStatus.IN_ARREARS) {
            loan.setLoanStatus(Loan.LoanStatus.ACTIVE);
        }

        loanRepository.save(loan);

        // 5. Create Transaction Record
        Transaction txn = Transaction.builder()
                .transactionId(referenceCodeService.generateReferenceCode())
                .loan(loan)
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amount)
                .paymentMethod(Transaction.PaymentMethod.valueOf(paymentMethod.toUpperCase()))
                .referenceCode(sourceReference)
                .description("Repayment: Principal=" + totalPrincipalPaid + ", Interest=" + totalInterestPaid)
                .balanceAfter(newTotalOutstanding)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);

        // 6. Post to Accounting
        try {
            accountingService.postLoanRepayment(loan, totalPrincipalPaid, totalInterestPaid);
        } catch (Exception e) {
            log.error("Failed to post GL entry for repayment", e);
        }
    }

    /**
     * ✅ COMPATIBILITY FIX: This method fixes the 'cannot find symbol' error.
     * It bridges the old legacy calls to the new smart repayment logic.
     */
    @Transactional
    public void processPayment(Loan loan, BigDecimal amount, String sourceAccountCode) {
        // Defaulting to "CASH" if not specified, since legacy calls didn't have PaymentMethod enum
        String method = "CASH";

        // Call the new logic
        processRepayment(loan.getId(), amount, method, sourceAccountCode);
    }
}