package com.sacco.sacco_system.service;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.*;
import com.sacco.sacco_system.repository.*;
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
    private final LoanProductRepository loanProductRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final GuarantorRepository guarantorRepository;
    private final ChargeRepository chargeRepository; // ✅ ADDED: For Fees

    // ========================================================================
    // 1. APPLICATION & CREATION
    // ========================================================================

    public LoanDTO applyForLoan(UUID memberId, UUID productId, BigDecimal principalAmount, Integer durationMonths) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Loan Product not found"));

        // 1. Check Limits
        if (principalAmount.compareTo(product.getMaxLimit()) > 0) {
            throw new RuntimeException("Amount exceeds product limit of " + product.getMaxLimit());
        }
        if (durationMonths > product.getMaxTenureMonths()) {
            throw new RuntimeException("Duration exceeds product limit of " + product.getMaxTenureMonths() + " months");
        }

        // 2. Calculate Interest & Installments
        BigDecimal totalInterest;
        BigDecimal monthlyRepayment;

        if (product.getInterestType() == LoanProduct.InterestType.FLAT_RATE) {
            // Flat Rate: Interest = P * R * T
            totalInterest = principalAmount
                    .multiply(product.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(durationMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));

            BigDecimal totalPayable = principalAmount.add(totalInterest);
            monthlyRepayment = totalPayable.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);

        } else {
            // Reducing Balance
            BigDecimal monthlyRate = product.getInterestRate().divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
            BigDecimal numerator = monthlyRate.multiply(BigDecimal.ONE.add(monthlyRate).pow(durationMonths));
            BigDecimal denominator = BigDecimal.ONE.add(monthlyRate).pow(durationMonths).subtract(BigDecimal.ONE);

            monthlyRepayment = principalAmount.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
            BigDecimal totalPayable = monthlyRepayment.multiply(BigDecimal.valueOf(durationMonths));
            totalInterest = totalPayable.subtract(principalAmount);
        }

        Loan loan = Loan.builder()
                .loanNumber(generateLoanNumber())
                .member(member)
                .product(product)
                .principalAmount(principalAmount)
                .loanBalance(principalAmount)
                .interestRate(product.getInterestRate())
                .totalInterest(totalInterest)
                .durationMonths(durationMonths)
                .monthlyRepayment(monthlyRepayment)
                .status(Loan.LoanStatus.PENDING)
                .build();

        Loan savedLoan = loanRepository.save(loan);
        createRepaymentSchedule(savedLoan);

        return convertToDTO(savedLoan);
    }

    public GuarantorDTO addGuarantor(UUID loanId, UUID memberId, BigDecimal guaranteeAmount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        Member guarantorMember = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));

        if (loan.getMember().getId().equals(memberId)) {
            throw new RuntimeException("Member cannot guarantee their own loan.");
        }

        if (guarantorMember.getTotalSavings().compareTo(guaranteeAmount) < 0) {
            throw new RuntimeException("Guarantor does not have sufficient savings.");
        }

        Guarantor guarantor = Guarantor.builder()
                .loan(loan)
                .member(guarantorMember)
                .guaranteeAmount(guaranteeAmount)
                .status(Guarantor.GuarantorStatus.ACCEPTED)
                .dateAccepted(LocalDate.now())
                .build();

        guarantorRepository.save(guarantor);

        return GuarantorDTO.builder()
                .id(guarantor.getId())
                .loanId(loan.getId())
                .memberId(guarantorMember.getId())
                .memberName(guarantorMember.getFirstName() + " " + guarantorMember.getLastName())
                .guaranteeAmount(guarantor.getGuaranteeAmount())
                .status(guarantor.getStatus().toString())
                .build();
    }

    // ========================================================================
    // 2. LIFECYCLE MANAGEMENT
    // ========================================================================

    public LoanDTO approveLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.PENDING) throw new RuntimeException("Loan is not pending approval.");

        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO rejectLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.PENDING) throw new RuntimeException("Only PENDING loans can be rejected.");

        loan.setStatus(Loan.LoanStatus.REJECTED);
        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.APPROVED) throw new RuntimeException("Loan must be APPROVED first.");

        // ✅ 1. Apply Processing Fee (If any)
        BigDecimal fee = loan.getProduct().getProcessingFee();
        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            // Record the Fee
            Charge charge = Charge.builder()
                    .member(loan.getMember())
                    .loan(loan)
                    .type(Charge.ChargeType.PROCESSING_FEE)
                    .amount(fee)
                    .status(Charge.ChargeStatus.PAID)
                    .description("Processing Fee for " + loan.getLoanNumber())
                    .build();
            chargeRepository.save(charge);

            // GL: Debit Cash (Fee Income), Credit Fee Income (4000)
            accountingService.postDoubleEntry("Processing Fee " + loan.getLoanNumber(), "FEE-" + loan.getLoanNumber(), "1001", "4000", fee);
        }

        // ✅ 2. Disburse Principal
        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setExpectedRepaymentDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        loanRepository.save(loan);

        // Transaction
        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_DISBURSEMENT)
                .amount(loan.getPrincipalAmount())
                .description("Disbursement: " + loan.getLoanNumber())
                .build();
        transactionRepository.save(tx);

        // GL: Debit Loans Receivable (1200), Credit Cash (1001)
        accountingService.postDoubleEntry("Disbursement " + loan.getLoanNumber(), tx.getTransactionId(), "1200", "1001", loan.getPrincipalAmount());

        return convertToDTO(loan);
    }

    public LoanRepayment repayLoan(UUID loanId, BigDecimal amount) {
        LoanRepayment repayment = loanRepaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(
                        loanId, LoanRepayment.RepaymentStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending repayments found."));

        processRepayment(repayment, amount);
        return repayment;
    }

    private void processRepayment(LoanRepayment repayment, BigDecimal amount) {
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

        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.LOAN_REPAYMENT)
                .amount(amount)
                .description("Repayment: " + loan.getLoanNumber())
                .build();
        transactionRepository.save(tx);

        accountingService.postDoubleEntry("Repayment " + loan.getLoanNumber(), tx.getTransactionId(), "1001", "1200", amount);
    }

    // ========================================================================
    // 3. ADVANCED FEATURES (Write-off, Restructure, Penalties)
    // ========================================================================

    public void writeOffLoan(UUID loanId, String reason) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        BigDecimal balance = loan.getLoanBalance();

        if (balance.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Loan has no balance to write off.");

        loan.setStatus(Loan.LoanStatus.WRITTEN_OFF);
        loan.setLoanBalance(BigDecimal.ZERO);
        loanRepository.save(loan);

        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .type(Transaction.TransactionType.REVERSAL)
                .amount(balance)
                .description("Write-Off: " + reason)
                .build();
        transactionRepository.save(tx);

        // GL: Debit Bad Debts (5011), Credit Loans Receivable (1200)
        accountingService.postDoubleEntry("Write-Off " + loan.getLoanNumber(), tx.getTransactionId(), "5011", "1200", balance);
    }

    public LoanDTO restructureLoan(UUID loanId, Integer newDurationMonths) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        loan.setDurationMonths(newDurationMonths);
        BigDecimal newMonthly = loan.getLoanBalance().divide(BigDecimal.valueOf(newDurationMonths), 2, RoundingMode.HALF_UP);
        loan.setMonthlyRepayment(newMonthly);

        List<LoanRepayment> pending = loanRepaymentRepository.findByLoanIdAndStatus(loanId, LoanRepayment.RepaymentStatus.PENDING);
        loanRepaymentRepository.deleteAll(pending);

        createRepaymentSchedule(loan);

        return convertToDTO(loanRepository.save(loan));
    }

    // ✅ NEW: AUTOMATIC PENALTY CHECK
    public void checkAndApplyPenalties() {
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.LoanStatus.DISBURSED);
        LocalDate today = LocalDate.now();

        for (Loan loan : activeLoans) {
            // Find overdue repayments
            List<LoanRepayment> overdueRepayments = loanRepaymentRepository.findByLoanIdAndStatus(loan.getId(), LoanRepayment.RepaymentStatus.PENDING)
                    .stream()
                    .filter(r -> r.getDueDate().isBefore(today))
                    .collect(Collectors.toList());

            if (!overdueRepayments.isEmpty()) {
                BigDecimal penaltyRate = loan.getProduct().getPenaltyRate();
                if (penaltyRate == null || penaltyRate.compareTo(BigDecimal.ZERO) == 0) continue;

                for (LoanRepayment repayment : overdueRepayments) {
                    // Logic to ensure we don't charge multiple times for the same month can be added here
                    // For now, we calculate penalty on the overdue amount
                    BigDecimal overdueAmount = loan.getMonthlyRepayment().subtract(repayment.getTotalPaid());
                    BigDecimal penalty = overdueAmount.multiply(penaltyRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    if (penalty.compareTo(BigDecimal.ZERO) > 0) {
                        // Increase Loan Balance
                        loan.setLoanBalance(loan.getLoanBalance().add(penalty));
                        loanRepository.save(loan);

                        // Record Charge
                        Charge charge = Charge.builder()
                                .member(loan.getMember())
                                .loan(loan)
                                .type(Charge.ChargeType.LATE_PAYMENT_PENALTY)
                                .amount(penalty)
                                .description("Penalty for due date " + repayment.getDueDate())
                                .build();
                        chargeRepository.save(charge);

                        // GL Posting: Debit Loans Receivable (Asset), Credit Penalty Income (4001)
                        accountingService.postDoubleEntry("Penalty " + loan.getLoanNumber(), "PEN-" + repayment.getId(), "1200", "4001", penalty);
                    }
                }
            }
        }
    }

    // ========================================================================
    // 4. HELPERS
    // ========================================================================

    private void createRepaymentSchedule(Loan loan) {
        LocalDate paymentDate = (loan.getDisbursementDate() != null) ? loan.getDisbursementDate().plusMonths(1) : LocalDate.now().plusMonths(1);

        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanRepayment repayment = LoanRepayment.builder()
                    .loan(loan)
                    .repaymentNumber(i)
                    .dueDate(paymentDate)
                    .status(LoanRepayment.RepaymentStatus.PENDING)
                    .build();
            loanRepaymentRepository.save(repayment);
            paymentDate = paymentDate.plusMonths(1);
        }
    }

    private String generateLoanNumber() { long count = loanRepository.count(); return "LN" + String.format("%06d", count + 1); }
    public BigDecimal getTotalDisbursedLoans() { return loanRepository.getTotalDisbursedLoans(); }
    public BigDecimal getTotalOutstandingLoans() { return loanRepository.getTotalOutstandingLoans(); }
    public BigDecimal getTotalInterestCollected() { return loanRepository.getTotalInterest(); }

    public LoanDTO getLoanById(UUID id) { return convertToDTO(loanRepository.findById(id).orElseThrow()); }
    public LoanDTO getLoanByNumber(String number) { return convertToDTO(loanRepository.findByLoanNumber(number).orElseThrow()); }
    public List<LoanDTO> getAllLoans() { return loanRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public List<LoanDTO> getLoansByMemberId(UUID id) { return loanRepository.findByMemberId(id).stream().map(this::convertToDTO).collect(Collectors.toList()); }
    public List<LoanDTO> getLoansByStatus(Loan.LoanStatus status) { return loanRepository.findByStatus(status).stream().map(this::convertToDTO).collect(Collectors.toList()); }

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