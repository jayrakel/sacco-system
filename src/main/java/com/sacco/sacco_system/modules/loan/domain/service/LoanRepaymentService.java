package com.sacco.sacco_system.modules.loan.domain.service;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;

import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanRepaymentService {

    private final LoanRepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final AccountingService accountingService;

    /**
     * âœ… Generates the repayment schedule based on Weeks/Months and Grace Period.
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
     * âœ… Processes a payment transaction with Prepayment/Arrears logic.
     * Logic: Puts money into a "pot" (Payment + Previous Overpayment),
     * then pays off Arrears -> Current Installment -> Future Overpayment.
     */
    public void processPayment(Loan loan, BigDecimal amountPaid) {
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
        loan.setLoanBalance(loan.getLoanBalance().subtract(amountPaid));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }

        loanRepository.save(loan);

        // ✅ POST TO ACCOUNTING - Creates journal entry for repayment
        // AccountingService will handle splitting into principal/interest internally
        accountingService.postLoanRepayment(loan, amountPaid);
        // Creates:
        //   DEBIT Cash (1020)
        //   CREDIT Loans Receivable (1100) - principal portion
        //   CREDIT Interest Income (4010) - interest portion
    }
}



