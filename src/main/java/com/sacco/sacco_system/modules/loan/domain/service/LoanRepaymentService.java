package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanRepaymentService {

    private final LoanRepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final ReferenceCodeService referenceCodeService;
    private final SystemSettingService systemSettingService;

    public void generateSchedule(Loan loan) {
        int gracePeriodWeeks = (int) systemSettingService.getDouble("LOAN_GRACE_PERIOD_WEEKS", 1.0);

        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal rate = loan.getProduct().getInterestRate();
        int duration = loan.getDuration();
        Loan.DurationUnit unit = loan.getDurationUnit() != null ? loan.getDurationUnit() : Loan.DurationUnit.WEEKS;

        BigDecimal installmentAmount;
        int totalInstallments;
        BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // 1. Calculate Interest & Installments based on Unit
        if (unit == Loan.DurationUnit.WEEKS) {
            totalInstallments = duration;
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            BigDecimal totalInterest = principal.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
            installmentAmount = principal.add(totalInterest).divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        } else {
            // Monthly
            totalInstallments = duration; // 12 Months = 12 Installments
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            BigDecimal totalInterest = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
            installmentAmount = principal.add(totalInterest).divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        }

        loan.setWeeklyRepaymentAmount(installmentAmount);

        // Set initial balance (Principal + Interest) -> Flat Rate Model
        BigDecimal totalRepayable = installmentAmount.multiply(BigDecimal.valueOf(totalInstallments));
        loan.setLoanBalance(totalRepayable);

        List<LoanRepayment> schedule = new ArrayList<>();
        LocalDate nextDueDate = LocalDate.now().plusWeeks(gracePeriodWeeks);

        for (int i = 1; i <= totalInstallments; i++) {
            // Increment Date based on Unit
            if (i > 1) {
                nextDueDate = (unit == Loan.DurationUnit.WEEKS) ? nextDueDate.plusWeeks(1) : nextDueDate.plusMonths(1);
            }

            LoanRepayment r = LoanRepayment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(nextDueDate)
                    .amountDue(installmentAmount)
                    .status(LoanRepayment.RepaymentStatus.PENDING)
                    .amountPaid(BigDecimal.ZERO)
                    .build();
            schedule.add(r);
        }

        repaymentRepository.saveAll(schedule);
        loanRepository.save(loan);
        log.info("Generated schedule for Loan {}: {} installments", loan.getLoanNumber(), totalInstallments);
    }

    public void processPayment(Loan loan, BigDecimal amountPaid, String sourceAccountCode) {
        // Pot includes current payment + any previously prepaid amount
        BigDecimal pot = amountPaid.add(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO);
        loan.setTotalPrepaid(BigDecimal.ZERO);

        // 1. Pay Arrears First
        if (loan.getTotalArrears() != null && loan.getTotalArrears().compareTo(BigDecimal.ZERO) > 0) {
            if (pot.compareTo(loan.getTotalArrears()) >= 0) {
                pot = pot.subtract(loan.getTotalArrears());
                loan.setTotalArrears(BigDecimal.ZERO);
            } else {
                loan.setTotalArrears(loan.getTotalArrears().subtract(pot));
                pot = BigDecimal.ZERO;
            }
        }

        // 2. Cascade Payment: Loop through pending installments
        // We use findByLoanId... but for simplicity, we can fetch all pending and sort
        List<LoanRepayment> pendingInstallments = repaymentRepository.findAll().stream()
                .filter(r -> r.getLoan().getId().equals(loan.getId()))
                .filter(r -> r.getStatus() == LoanRepayment.RepaymentStatus.PENDING || r.getStatus() == LoanRepayment.RepaymentStatus.PARTIALLY_PAID)
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .toList();

        for (LoanRepayment next : pendingInstallments) {
            if (pot.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal alreadyPaid = next.getAmountPaid() != null ? next.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal remainingOnInstallment = next.getAmountDue().subtract(alreadyPaid);

            if (pot.compareTo(remainingOnInstallment) >= 0) {
                // Fully pay this installment
                next.setAmountPaid(next.getAmountDue());
                next.setStatus(LoanRepayment.RepaymentStatus.PAID);
                next.setPaymentDate(LocalDate.now());
                pot = pot.subtract(remainingOnInstallment);
            } else {
                // Partially pay this installment
                next.setAmountPaid(alreadyPaid.add(pot));
                next.setStatus(LoanRepayment.RepaymentStatus.PARTIALLY_PAID);
                pot = BigDecimal.ZERO; // Pot exhausted
            }
            repaymentRepository.save(next);
        }

        // 3. Store remaining excess in Prepaid
        if (pot.compareTo(BigDecimal.ZERO) > 0) {
            loan.setTotalPrepaid(pot);
        }

        // 4. Update Loan Balance
        loan.setLoanBalance(loan.getLoanBalance().subtract(amountPaid));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setLoanBalance(BigDecimal.ZERO);
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }
        loanRepository.save(loan);

        // 5. Record Transaction & Post Accounting
        recordTransaction(loan, amountPaid, sourceAccountCode);
        accountingService.postEvent(
                "LOAN_REPAYMENT_PRINCIPAL",
                "Repayment - " + loan.getLoanNumber(),
                loan.getLoanNumber(),
                amountPaid,
                sourceAccountCode
        );
    }

    private void recordTransaction(Loan loan, BigDecimal amount, String sourceAccount) {
        Transaction.PaymentMethod method = Transaction.PaymentMethod.CASH;
        if ("1002".equals(sourceAccount)) method = Transaction.PaymentMethod.MPESA;
        else if (sourceAccount != null && sourceAccount.startsWith("101")) method = Transaction.PaymentMethod.BANK;

        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amount)
                .paymentMethod(method)
                .referenceCode(referenceCodeService.generateReferenceCode())
                .description("Loan repayment - " + loan.getLoanNumber())
                .balanceAfter(loan.getLoanBalance())
                .build();
        transactionRepository.save(tx);
    }
}