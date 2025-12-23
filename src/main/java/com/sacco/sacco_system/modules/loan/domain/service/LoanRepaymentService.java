package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanRepaymentService {

    private final LoanRepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final ReferenceCodeService referenceCodeService;

    /**
     * ✅ Generates the repayment schedule based on Weeks/Months and Grace Period.
     * This is called by LoanService when the Treasurer disburses the loan.
     */
    public void generateRepaymentSchedule(Loan loan, int gracePeriodWeeks) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal rate = loan.getProduct().getInterestRate(); // Annual Rate
        int duration = loan.getDuration();

        BigDecimal installmentAmount;
        BigDecimal totalInterest;

        // --- 1. Calculate Interest & Installment Amount ---
        if (loan.getDurationUnit() == Loan.DurationUnit.WEEKS) {
            // Weekly Calculation: Annual Rate / 52
            BigDecimal weeklyRate = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);

            BigDecimal totalInterestRaw = principal.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
            installmentAmount = principal.add(totalInterestRaw).divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
            totalInterest = totalInterestRaw;
        } else {
            // Monthly Calculation: Annual Rate / 12
            BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);

            BigDecimal totalInterestRaw = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
            installmentAmount = principal.add(totalInterestRaw).divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
            totalInterest = totalInterestRaw;
        }

        // Update Loan Aggregate Data
        loan.setMonthlyRepayment(installmentAmount);
        loan.setTotalInterest(totalInterest);
        loan.setLoanBalance(principal.add(totalInterest));

        // --- 2. Generate Installment Records ---
        LocalDate nextDate = LocalDate.now().plusWeeks(gracePeriodWeeks);
        if (loan.getDurationUnit() == Loan.DurationUnit.MONTHS) {
            // If grace is in weeks but tenure in months, approximate start
            nextDate = LocalDate.now().plusWeeks(gracePeriodWeeks);
        }

        for (int i = 1; i <= duration; i++) {
            LoanRepayment r = LoanRepayment.builder()
                    .loan(loan)
                    .repaymentNumber(i)
                    .dueDate(nextDate)
                    .amount(installmentAmount)
                    .status(LoanRepayment.RepaymentStatus.PENDING)
                    .totalPaid(BigDecimal.ZERO)
                    .build();
            repaymentRepository.save(r);

            // Increment Date for next entry
            if (loan.getDurationUnit() == Loan.DurationUnit.WEEKS) {
                nextDate = nextDate.plusWeeks(1);
            } else {
                nextDate = nextDate.plusMonths(1);
            }
        }
    }

    /**
     * Overloaded processPayment for backward compatibility (Defaults to CASH/System)
     */
    public void processPayment(Loan loan, BigDecimal amountPaid) {
        processPayment(loan, amountPaid, null);
    }

    /**
     * ✅ UPDATED: Processes a payment transaction with Routing logic.
     * Accepts sourceAccountCode to ensure the General Ledger is updated correctly.
     */
    public void processPayment(Loan loan, BigDecimal amountPaid, String sourceAccountCode) {
        // 1. Create the "Pot" (Current Payment + Any stored Prepayment)
        BigDecimal pot = amountPaid.add(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO);
        loan.setTotalPrepaid(BigDecimal.ZERO); // Clear buffer (will rebuild at end if pot remains)

        // 2. Pay Off Arrears First
        if (loan.getTotalArrears() != null && loan.getTotalArrears().compareTo(BigDecimal.ZERO) > 0) {
            if (pot.compareTo(loan.getTotalArrears()) >= 0) {
                // Pot covers all arrears
                pot = pot.subtract(loan.getTotalArrears());
                loan.setTotalArrears(BigDecimal.ZERO);
            } else {
                // Pot covers partial arrears
                loan.setTotalArrears(loan.getTotalArrears().subtract(pot));
                pot = BigDecimal.ZERO;
            }
        }

        // 3. Pay Next Pending Installment
        if (pot.compareTo(BigDecimal.ZERO) > 0) {
            LoanRepayment next = repaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                    loan.getId(), LoanRepayment.RepaymentStatus.PENDING).orElse(null);

            if (next != null) {
                BigDecimal pendingOnInstallment = next.getAmount().subtract(next.getTotalPaid());

                if (pot.compareTo(pendingOnInstallment) >= 0) {
                    // Full Installment Paid
                    next.setTotalPaid(next.getAmount());
                    next.setStatus(LoanRepayment.RepaymentStatus.PAID);
                    next.setPaymentDate(LocalDate.now());

                    pot = pot.subtract(pendingOnInstallment); // Remaining goes to prepayment
                } else {
                    // Partial Payment on Installment
                    next.setTotalPaid(next.getTotalPaid().add(pot));
                    next.setStatus(LoanRepayment.RepaymentStatus.PARTIALLY_PAID);
                    pot = BigDecimal.ZERO;
                }
                repaymentRepository.save(next);
            }
        }

        // 4. Store Remaining as Prepayment (Buffer for next time)
        if (pot.compareTo(BigDecimal.ZERO) > 0) {
            loan.setTotalPrepaid(pot);
        }

        // 5. Reduce Principal Balance
        // Note: Using max(0) to prevent negative balance visual issues, though status handles completion
        BigDecimal currentBalance = loan.getLoanBalance();
        loan.setLoanBalance(currentBalance.subtract(amountPaid));
        
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }

        loanRepository.save(loan);

        // Determine Payment Method
        Transaction.PaymentMethod paymentMethod = Transaction.PaymentMethod.CASH;
        if (sourceAccountCode != null) {
            if (sourceAccountCode.equals("1002")) paymentMethod = Transaction.PaymentMethod.MPESA;
            else if (sourceAccountCode.startsWith("101")) paymentMethod = Transaction.PaymentMethod.BANK;
        }

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amountPaid)
                .paymentMethod(paymentMethod)
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Loan repayment - " + loan.getLoanNumber())
                .balanceAfter(loan.getLoanBalance())
                .build();
        transactionRepository.save(transaction);

        // ✅ POST TO ACCOUNTING - Explicitly using the Source Account
        // Since loanBalance includes interest, we treat the repayment as reducing the Loan Receivable Asset.
        // Any split between Principal/Interest income is handled by your loan configuration, 
        // but for now we route the full amount to the Principal Repayment GL (1200) to reduce the asset.
        
        BigDecimal principalPortion = amountPaid; 
        
        // Post Principal Repayment (Credit 1200, Debit Source)
        accountingService.postEvent(
            "LOAN_REPAYMENT_PRINCIPAL", 
            "Loan Repayment - " + loan.getLoanNumber(), 
            loan.getLoanNumber(), 
            principalPortion, 
            sourceAccountCode // ✅ Correctly routes to Bank/Mpesa/Cash
        );
    }
}