package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.*;
import com.sacco.sacco_system.repository.LoanRepository;
import com.sacco.sacco_system.repository.LoanRepaymentRepository;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.repository.TransactionRepository; // ✅ Import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;

    // ✅ ADDED: Required for Recording Transactions & GL
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;

    public LoanDTO applyForLoan(UUID memberId, BigDecimal principalAmount,
                                BigDecimal interestRate, Integer durationMonths) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        BigDecimal totalDeposits = member.getTotalSavings().add(member.getTotalShares());
        BigDecimal maxLoanLimit = totalDeposits.multiply(BigDecimal.valueOf(3));

        if (principalAmount.compareTo(maxLoanLimit) > 0) {
            throw new RuntimeException("Loan refused. Maximum limit is " + maxLoanLimit +
                    " (3x your savings of " + totalDeposits + ")");
        }

        BigDecimal totalInterest = principalAmount
                .multiply(interestRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(durationMonths))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = principalAmount.add(totalInterest);
        BigDecimal monthlyRepayment = totalAmount
                .divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .member(member)
                .principalAmount(principalAmount)
                .loanBalance(principalAmount)
                .interestRate(interestRate)
                .totalInterest(totalInterest)
                .durationMonths(durationMonths)
                .monthlyRepayment(monthlyRepayment)
                .status(Loan.LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(loan);
        createRepaymentSchedule(savedLoan);

        return convertToDTO(savedLoan);
    }

    // ... (Getters remain unchanged) ...
    public LoanDTO getLoanById(UUID id) { return convertToDTO(loanRepository.findById(id).orElseThrow(() -> new RuntimeException("Loan not found"))); }
    public LoanDTO getLoanByNumber(String loanNumber) { return convertToDTO(loanRepository.findByLoanNumber(loanNumber).orElseThrow(() -> new RuntimeException("Loan not found"))); }
    public List<LoanDTO> getLoansByMemberId(UUID memberId) { return loanRepository.findByMemberId(memberId).stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public List<LoanDTO> getAllLoans() { return loanRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public List<LoanDTO> getLoansByStatus(Loan.LoanStatus status) { return loanRepository.findByStatus(status).stream().map(this::convertToDTO).collect(Collectors.toList()); }

    public LoanDTO approveLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.getStatus().equals(Loan.LoanStatus.APPROVED)) {
            throw new RuntimeException("Only approved loans can be disbursed");
        }

        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setExpectedRepaymentDate(LocalDate.now().plusMonths(loan.getDurationMonths()));

        Loan savedLoan = loanRepository.save(loan);

        // ✅ 1. Create Transaction Record (User History)
        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getPrincipalAmount())
                .description("Loan Disbursement: " + loan.getLoanNumber())
                // No savings account involved in direct disbursement, or link to main
                .build();
        transactionRepository.save(tx);

        // ✅ 2. POST TO GL (Double Entry)
        // Debit: Loans Receivable (1200) | Credit: Cash/Bank (1001)
        accountingService.postDoubleEntry(
                "Disbursement - " + loan.getLoanNumber(),
                tx.getTransactionId(),
                "1200", // DEBIT (Asset increases)
                "1001", // CREDIT (Cash decreases)
                loan.getPrincipalAmount()
        );

        return convertToDTO(savedLoan);
    }

    public LoanDTO rejectLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(Loan.LoanStatus.REJECTED);
        return convertToDTO(loanRepository.save(loan));
    }

    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        Member guarantorMember = memberRepository.findById(guarantorMemberId).orElseThrow(() -> new RuntimeException("Guarantor not found"));

        if (loan.getMember().getId().equals(guarantorMember.getId())) {
            throw new RuntimeException("You cannot guarantee your own loan.");
        }

        Guarantor guarantor = Guarantor.builder().loan(loan).member(guarantorMember).guaranteeAmount(amount).build();
        loan.getGuarantors().add(guarantor);
        loanRepository.save(loan);

        return GuarantorDTO.builder().loanId(loan.getId()).memberId(guarantorMember.getId())
                .memberName(guarantorMember.getFirstName() + " " + guarantorMember.getLastName()).guaranteeAmount(amount).build();
    }

    public LoanRepayment repayLoan(UUID loanId, BigDecimal amount) {
        LoanRepayment repayment = loanRepaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                        loanId, LoanRepayment.RepaymentStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending repayments found. Loan might be fully paid!"));

        BigDecimal dueAmount = repayment.getLoan().getMonthlyRepayment();
        if (amount.compareTo(dueAmount) < 0) {
            throw new RuntimeException("Insufficient amount. Monthly installment is: " + dueAmount);
        }

        return processRepayment(repayment, amount);
    }

    public void recordRepayment(UUID repaymentId, BigDecimal amount) {
        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));
        processRepayment(repayment, amount);
    }

    // ✅ HELPER: Handles Repayment Logic + Accounting
    private LoanRepayment processRepayment(LoanRepayment repayment, BigDecimal amount) {
        Loan loan = repayment.getLoan();

        repayment.setPrincipalPaid(amount);
        repayment.setTotalPaid(amount);
        repayment.setStatus(LoanRepayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDate.now());
        loanRepaymentRepository.save(repayment);

        loan.setLoanBalance(loan.getLoanBalance().subtract(amount));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.COMPLETED);
        }
        loanRepository.save(loan);

        // ✅ 1. Record Transaction (User History)
        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amount)
                .description("Repayment for Loan: " + loan.getLoanNumber())
                .build();
        transactionRepository.save(tx);

        // ✅ 2. POST TO GL (Double Entry)
        // Debit: Cash (1001) | Credit: Loans Receivable (1200)
        accountingService.postDoubleEntry(
                "Repayment - " + loan.getLoanNumber(),
                tx.getTransactionId(),
                "1001", // DEBIT (Cash increases)
                "1200", // CREDIT (Asset decreases)
                amount
        );

        return repayment;
    }

    // ... (Keep statistics getters and helpers) ...
    public BigDecimal getTotalDisbursedLoans() { return loanRepository.getTotalDisbursedLoans(); }
    public BigDecimal getTotalOutstandingLoans() { return loanRepository.getTotalOutstandingLoans(); }
    public BigDecimal getTotalInterestCollected() { return loanRepository.getTotalInterest(); }
    private String generateLoanNumber() { long count = loanRepository.count(); return "LN" + String.format("%06d", count + 1); }

    private void createRepaymentSchedule(Loan loan) {
        LocalDate paymentDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanRepayment repayment = LoanRepayment.builder().loan(loan).repaymentNumber(i).dueDate(paymentDate).status(LoanRepayment.RepaymentStatus.PENDING).build();
            loanRepaymentRepository.save(repayment);
            paymentDate = paymentDate.plusMonths(1);
        }
    }

    private LoanDTO convertToDTO(Loan loan) {
        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(loan.getMember().getId())
                .memberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .interestRate(loan.getInterestRate())
                .totalInterest(loan.getTotalInterest())
                .durationMonths(loan.getDurationMonths())
                .monthlyRepayment(loan.getMonthlyRepayment())
                .status(loan.getStatus().toString())
                .approvalDate(loan.getApprovalDate())
                .disbursementDate(loan.getDisbursementDate())
                .expectedRepaymentDate(loan.getExpectedRepaymentDate())
                .build();
    }
}