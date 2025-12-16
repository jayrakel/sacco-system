package com.sacco.sacco_system.service;

import com.sacco.sacco_system.annotation.Loggable; // ✅ Automated Audit
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
    private final ChargeRepository chargeRepository;

    @Loggable(action = "APPLY_LOAN", category = "LOANS")
    public LoanDTO applyForLoan(UUID memberId, UUID productId, BigDecimal principalAmount, Integer durationMonths) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow(() -> new RuntimeException("Loan Product not found"));

        if (principalAmount.compareTo(product.getMaxLimit()) > 0) throw new RuntimeException("Amount exceeds product limit");
        if (durationMonths > product.getMaxTenureMonths()) throw new RuntimeException("Duration exceeds product limit");

        BigDecimal totalInterest;
        BigDecimal monthlyRepayment;

        if (product.getInterestType() == LoanProduct.InterestType.FLAT_RATE) {
            totalInterest = principalAmount
                    .multiply(product.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                    .multiply(BigDecimal.valueOf(durationMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP));
            BigDecimal totalPayable = principalAmount.add(totalInterest);
            monthlyRepayment = totalPayable.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
        } else {
            BigDecimal monthlyRate = product.getInterestRate().divide(BigDecimal.valueOf(1200), 8, RoundingMode.HALF_UP);
            BigDecimal numerator = monthlyRate.multiply(BigDecimal.ONE.add(monthlyRate).pow(durationMonths));
            BigDecimal denominator = BigDecimal.ONE.add(monthlyRate).pow(durationMonths).subtract(BigDecimal.ONE);
            monthlyRepayment = principalAmount.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
            totalInterest = monthlyRepayment.multiply(BigDecimal.valueOf(durationMonths)).subtract(principalAmount);
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
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantorMember = memberRepository.findById(memberId).orElseThrow();

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
        return GuarantorDTO.builder().id(guarantor.getId()).build();
    }

    @Loggable(action = "APPROVE_LOAN", category = "LOANS")
    public LoanDTO approveLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());
        return convertToDTO(loanRepository.save(loan));
    }

    @Loggable(action = "REJECT_LOAN", category = "LOANS")
    public LoanDTO rejectLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.REJECTED);
        return convertToDTO(loanRepository.save(loan));
    }

    @Loggable(action = "DISBURSE_LOAN", category = "LOANS")
    public LoanDTO disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (loan.getStatus() != Loan.LoanStatus.APPROVED) throw new RuntimeException("Loan must be APPROVED.");

        BigDecimal fee = loan.getProduct().getProcessingFee();
        if (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) {
            Charge charge = Charge.builder().member(loan.getMember()).loan(loan).type(Charge.ChargeType.PROCESSING_FEE).amount(fee).status(Charge.ChargeStatus.PAID).description("Processing Fee").build();
            chargeRepository.save(charge);

            // ✅ DYNAMIC ACCOUNTING
            accountingService.postEvent("PROCESSING_FEE", "Fee " + loan.getLoanNumber(), "FEE-" + loan.getLoanNumber(), fee);
        }

        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setExpectedRepaymentDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        loanRepository.save(loan);

        Transaction tx = Transaction.builder().member(loan.getMember()).type(Transaction.TransactionType.LOAN_DISBURSEMENT).amount(loan.getPrincipalAmount()).description("Disbursement").build();
        transactionRepository.save(tx);

        // ✅ DYNAMIC ACCOUNTING
        accountingService.postEvent("LOAN_DISBURSEMENT", "Disbursement " + loan.getLoanNumber(), tx.getTransactionId(), loan.getPrincipalAmount());

        return convertToDTO(loan);
    }

    @Loggable(action = "LOAN_REPAYMENT", category = "LOANS")
    public LoanRepayment repayLoan(UUID loanId, BigDecimal amount) {
        LoanRepayment repayment = loanRepaymentRepository.findFirstByLoanIdAndStatusOrderByDueDateAsc(loanId, LoanRepayment.RepaymentStatus.PENDING).orElseThrow();
        Loan loan = repayment.getLoan();

        repayment.setPrincipalPaid(amount);
        repayment.setTotalPaid(amount);
        repayment.setStatus(LoanRepayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDate.now());
        loanRepaymentRepository.save(repayment);

        loan.setLoanBalance(loan.getLoanBalance().subtract(amount));
        if (loan.getLoanBalance().compareTo(BigDecimal.ZERO) <= 0) loan.setStatus(Loan.LoanStatus.COMPLETED);
        loanRepository.save(loan);

        Transaction tx = Transaction.builder().member(loan.getMember()).type(Transaction.TransactionType.LOAN_REPAYMENT).amount(amount).description("Repayment").build();
        transactionRepository.save(tx);

        // ✅ DYNAMIC ACCOUNTING
        accountingService.postEvent("LOAN_REPAYMENT", "Repayment " + loan.getLoanNumber(), tx.getTransactionId(), amount);

        return repayment;
    }

    @Loggable(action = "WRITE_OFF_LOAN", category = "LOANS")
    public void writeOffLoan(UUID loanId, String reason) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        BigDecimal balance = loan.getLoanBalance();

        loan.setStatus(Loan.LoanStatus.WRITTEN_OFF);
        loan.setLoanBalance(BigDecimal.ZERO);
        loanRepository.save(loan);

        Transaction tx = Transaction.builder().member(loan.getMember()).type(Transaction.TransactionType.REVERSAL).amount(balance).description("Write-Off: " + reason).build();
        transactionRepository.save(tx);

        // ✅ DYNAMIC ACCOUNTING
        accountingService.postEvent("WRITE_OFF", "Write-Off " + loan.getLoanNumber(), tx.getTransactionId(), balance);
    }

    @Loggable(action = "RESTRUCTURE_LOAN", category = "LOANS")
    public LoanDTO restructureLoan(UUID loanId, Integer newDurationMonths) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDurationMonths(newDurationMonths);
        BigDecimal newMonthly = loan.getLoanBalance().divide(BigDecimal.valueOf(newDurationMonths), 2, RoundingMode.HALF_UP);
        loan.setMonthlyRepayment(newMonthly);

        List<LoanRepayment> pending = loanRepaymentRepository.findByLoanIdAndStatus(loanId, LoanRepayment.RepaymentStatus.PENDING);
        loanRepaymentRepository.deleteAll(pending);

        createRepaymentSchedule(loan);
        return convertToDTO(loanRepository.save(loan));
    }

    public void checkAndApplyPenalties() {
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.LoanStatus.DISBURSED);
        LocalDate today = LocalDate.now();

        for (Loan loan : activeLoans) {
            List<LoanRepayment> overdueRepayments = loanRepaymentRepository.findByLoanIdAndStatus(loan.getId(), LoanRepayment.RepaymentStatus.PENDING)
                    .stream().filter(r -> r.getDueDate().isBefore(today)).collect(Collectors.toList());

            if (!overdueRepayments.isEmpty()) {
                BigDecimal penaltyRate = loan.getProduct().getPenaltyRate();
                if (penaltyRate != null && penaltyRate.compareTo(BigDecimal.ZERO) > 0) {
                    for (LoanRepayment repayment : overdueRepayments) {
                        BigDecimal overdueAmount = loan.getMonthlyRepayment().subtract(repayment.getTotalPaid());
                        BigDecimal penalty = overdueAmount.multiply(penaltyRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
                            loan.setLoanBalance(loan.getLoanBalance().add(penalty));
                            loanRepository.save(loan);
                            Charge charge = Charge.builder().member(loan.getMember()).loan(loan).type(Charge.ChargeType.LATE_PAYMENT_PENALTY).amount(penalty).description("Penalty").build();
                            chargeRepository.save(charge);

                            // ✅ DYNAMIC ACCOUNTING
                            accountingService.postEvent("PENALTY_CHARGED", "Penalty " + loan.getLoanNumber(), "PEN-" + repayment.getId(), penalty);
                        }
                    }
                }
            }
        }
    }

    private void createRepaymentSchedule(Loan loan) {
        LocalDate paymentDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= loan.getDurationMonths(); i++) {
            LoanRepayment repayment = LoanRepayment.builder().loan(loan).repaymentNumber(i).dueDate(paymentDate).status(LoanRepayment.RepaymentStatus.PENDING).build();
            loanRepaymentRepository.save(repayment);
            paymentDate = paymentDate.plusMonths(1);
        }
    }

    private String generateLoanNumber() { return "LN" + String.format("%06d", loanRepository.count() + 1); }
    public BigDecimal getTotalDisbursedLoans() { return loanRepository.getTotalDisbursedLoans(); }
    public BigDecimal getTotalOutstandingLoans() { return loanRepository.getTotalOutstandingLoans(); }
    public BigDecimal getTotalInterestCollected() { return loanRepository.getTotalInterest(); }
    public LoanDTO getLoanById(UUID id) { return convertToDTO(loanRepository.findById(id).orElseThrow()); }
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