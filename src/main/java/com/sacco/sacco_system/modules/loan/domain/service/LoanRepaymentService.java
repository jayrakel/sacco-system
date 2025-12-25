package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
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

        BigDecimal weeklyInstallment;
        int totalInstallments;

        BigDecimal rateDecimal = rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        if (loan.getDurationUnit() == Loan.DurationUnit.WEEKS) {
            totalInstallments = duration;
            BigDecimal weeklyRate = rateDecimal.divide(BigDecimal.valueOf(52), 8, RoundingMode.HALF_UP);
            BigDecimal totalInterest = principal.multiply(weeklyRate).multiply(BigDecimal.valueOf(duration));
            weeklyInstallment = principal.add(totalInterest).divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        } else {
            totalInstallments = (int) Math.ceil(duration * 4.33);
            BigDecimal monthlyRate = rateDecimal.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
            BigDecimal totalInterest = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(duration));
            weeklyInstallment = principal.add(totalInterest).divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        }

        loan.setWeeklyRepaymentAmount(weeklyInstallment);
        loan.setLoanBalance(principal.add(principal.multiply(rateDecimal).multiply(BigDecimal.valueOf(duration)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)));

        List<LoanRepayment> schedule = new ArrayList<>();
        LocalDate nextDueDate = LocalDate.now().plusWeeks(gracePeriodWeeks + 1);

        for (int i = 1; i <= totalInstallments; i++) {
            LoanRepayment r = LoanRepayment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(nextDueDate)
                    .amountDue(weeklyInstallment)
                    .status(LoanRepayment.RepaymentStatus.PENDING)
                    .amountPaid(BigDecimal.ZERO)
                    .build();
            schedule.add(r);
            nextDueDate = nextDueDate.plusWeeks(1);
        }
        
        repaymentRepository.saveAll(schedule);
        loanRepository.save(loan);
        log.info("Generated schedule for Loan {}: {} installments", loan.getLoanNumber(), totalInstallments);
    }

    public void processPayment(Loan loan, BigDecimal amountPaid, String sourceAccountCode) {
        final BigDecimal[] pot = { amountPaid.add(loan.getTotalPrepaid() != null ? loan.getTotalPrepaid() : BigDecimal.ZERO) };
        loan.setTotalPrepaid(BigDecimal.ZERO);

        // 1. Pay Arrears
        if (loan.getTotalArrears() != null && loan.getTotalArrears().compareTo(BigDecimal.ZERO) > 0) {
            if (pot[0].compareTo(loan.getTotalArrears()) >= 0) {
                pot[0] = pot[0].subtract(loan.getTotalArrears());
                loan.setTotalArrears(BigDecimal.ZERO);
            } else {
                loan.setTotalArrears(loan.getTotalArrears().subtract(pot[0]));
                pot[0] = BigDecimal.ZERO;
            }
        }

        // 2. Pay Next Installment
        repaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                loan.getId(), LoanRepayment.RepaymentStatus.PENDING).ifPresent(next -> {
            
            BigDecimal remaining = next.getAmountDue().subtract(next.getAmountPaid() != null ? next.getAmountPaid() : BigDecimal.ZERO);

            if (pot[0].compareTo(remaining) >= 0) {
                next.setAmountPaid(next.getAmountDue());
                next.setStatus(LoanRepayment.RepaymentStatus.PAID);
                next.setPaymentDate(LocalDate.now());
                loan.setTotalPrepaid(pot[0].subtract(remaining));
            } else {
                next.setAmountPaid((next.getAmountPaid() != null ? next.getAmountPaid() : BigDecimal.ZERO).add(pot[0]));
                next.setStatus(LoanRepayment.RepaymentStatus.PARTIALLY_PAID);
                pot[0] = BigDecimal.ZERO;
            }
            repaymentRepository.save(next);
        });

        // 3. Update Loan Balance
        loan.setLoanBalance(loan.getLoanBalance().subtract(amountPaid));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setLoanBalance(BigDecimal.ZERO);
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }
        loanRepository.save(loan);

        // 4. Record Transaction & Post Accounting
        recordTransaction(loan, amountPaid, sourceAccountCode);
        accountingService.postEvent(
            "LOAN_REPAYMENT_PRINCIPAL", 
            "Repayment - " + loan.getLoanNumber(), 
            loan.getLoanNumber(), 
            amountPaid, 
            sourceAccountCode
        );
    } // ✅ This was the missing brace causing the error

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