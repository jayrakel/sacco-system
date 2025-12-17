package com.sacco.sacco_system.service;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.*;
import com.sacco.sacco_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final GuarantorRepository guarantorRepository;
    private final TransactionRepository transactionRepository;
    private final AccountingService accountingService;
    private final LoanRepaymentService repaymentService;
    private final SystemSettingService systemSettingService;

    // ========================================================================
    // 1. MEMBER: APPLICATION PHASE
    // ========================================================================

    @Loggable(action = "INITIATE_APPLICATION", category = "LOANS")
    public LoanDTO initiateApplication(UUID memberId, UUID productId, BigDecimal amount, Integer duration, String unit) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        if (amount.compareTo(product.getMaxLimit()) > 0) throw new RuntimeException("Amount exceeds product limit");

        Loan loan = Loan.builder()
                .loanNumber("LN" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(amount)
                .duration(duration)
                .durationUnit(Loan.DurationUnit.valueOf(unit))
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .votesYes(0).votesNo(0)
                .totalPrepaid(BigDecimal.ZERO).totalArrears(BigDecimal.ZERO)
                .build();

        return convertToDTO(loanRepository.save(loan));
    }

    // Legacy support for older controller calls (if any exist) defaulting to MONTHS
    public LoanDTO applyForLoan(UUID memberId, UUID productId, BigDecimal amount, Integer duration) {
        return initiateApplication(memberId, productId, amount, duration, "MONTHS");
    }

    @Loggable(action = "SUBMIT_TO_GUARANTORS", category = "LOANS")
    public void submitToGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if(loan.getGuarantors() == null || loan.getGuarantors().isEmpty())
            throw new RuntimeException("At least one guarantor required");

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        loanRepository.save(loan);
    }

    @Loggable(action = "ADD_GUARANTOR", category = "LOANS")
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();

        if(guarantor.getId().equals(loan.getMember().getId()))
            throw new RuntimeException("Cannot guarantee self.");

        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        Guarantor saved = guarantorRepository.save(g);

        return GuarantorDTO.builder()
                .id(saved.getId())
                .memberId(saved.getMember().getId())
                .memberName(saved.getMember().getFirstName() + " " + saved.getMember().getLastName())
                .guaranteeAmount(saved.getGuaranteeAmount())
                .status(saved.getStatus().toString())
                .build();
    }

    // ========================================================================
    // 2. GUARANTORS & FEE
    // ========================================================================

    @Loggable(action = "GUARANTOR_RESPOND", category = "LOANS")
    public void guarantorRespond(UUID guarantorId, boolean accepted) {
        Guarantor g = guarantorRepository.findById(guarantorId).orElseThrow();
        g.setStatus(accepted ? Guarantor.GuarantorStatus.ACCEPTED : Guarantor.GuarantorStatus.DECLINED);
        g.setDateResponded(LocalDate.now());
        guarantorRepository.save(g);

        Loan loan = g.getLoan();
        long pending = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.PENDING);
        long declined = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.DECLINED);

        if (pending == 0 && declined == 0) {
            loan.setStatus(Loan.LoanStatus.GUARANTORS_APPROVED);
            loanRepository.save(loan);
        }
    }

    @Loggable(action = "PAY_APPLICATION_FEE", category = "LOANS")
    public void payApplicationFee(UUID loanId, String refCode) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if(loan.getStatus() != Loan.LoanStatus.GUARANTORS_APPROVED)
            throw new RuntimeException("Guarantors must approve before fee payment");

        BigDecimal fee = loan.getProduct().getProcessingFee();
        if (fee == null) fee = BigDecimal.ZERO;

        Transaction tx = Transaction.builder().member(loan.getMember()).amount(fee).type(Transaction.TransactionType.PROCESSING_FEE).referenceCode(refCode).build();
        transactionRepository.save(tx);
        accountingService.postEvent("PROCESSING_FEE", "Loan Fee " + loan.getLoanNumber(), refCode, fee);

        loan.setApplicationFeePaid(true);
        loan.setStatus(Loan.LoanStatus.SUBMITTED);
        loan.setSubmissionDate(LocalDate.now());
        loanRepository.save(loan);
    }

    // ========================================================================
    // 3. WORKFLOW
    // ========================================================================

    @Loggable(action = "OFFICER_APPROVE", category = "LOANS")
    public void officerApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loanRepository.save(loan);
    }

    @Loggable(action = "OPEN_VOTING", category = "LOANS")
    public void openVoting(UUID loanId, LocalDate meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setMeetingDate(meetingDate);
        loan.setVotingOpen(true);
        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        loanRepository.save(loan);
    }

    @Loggable(action = "CAST_VOTE", category = "LOANS")
    public void castVote(UUID loanId, boolean voteYes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if(!loan.isVotingOpen()) throw new RuntimeException("Voting is closed");

        if(voteYes) loan.setVotesYes(loan.getVotesYes() + 1);
        else loan.setVotesNo(loan.getVotesNo() + 1);

        loanRepository.save(loan);
    }

    @Loggable(action = "SECRETARY_FINALIZE", category = "LOANS")
    public void secretaryFinalize(UUID loanId, boolean approved, String comments) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setVotingOpen(false);
        loan.setSecretaryComments(comments);
        loan.setStatus(Loan.LoanStatus.VOTING_CLOSED);

        if(approved) {
            loan.setStatus(Loan.LoanStatus.ADMIN_APPROVED);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason(comments);
        }
        loanRepository.save(loan);
    }

    @Loggable(action = "ADMIN_APPROVE", category = "LOANS")
    public void adminApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loan.setApprovalDate(LocalDate.now());
        loanRepository.save(loan);
    }

    @Loggable(action = "DISBURSE_LOAN", category = "LOANS")
    public void treasurerDisburse(UUID loanId, String checkNumber) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        int graceWeeks = 1;
        try {
            graceWeeks = Integer.parseInt(systemSettingService.getSetting("LOAN_GRACE_PERIOD_WEEKS").orElse("1"));
        } catch (Exception e) {}

        repaymentService.generateRepaymentSchedule(loan, graceWeeks);
        loan.setGracePeriodWeeks(graceWeeks);
        loan.setCheckNumber(checkNumber);

        Transaction tx = Transaction.builder().member(loan.getMember()).amount(loan.getPrincipalAmount()).type(Transaction.TransactionType.LOAN_DISBURSEMENT).referenceCode(checkNumber).build();
        transactionRepository.save(tx);
        accountingService.postEvent("LOAN_DISBURSEMENT", "Disbursement " + checkNumber, checkNumber, loan.getPrincipalAmount());

        loan.setStatus(Loan.LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loanRepository.save(loan);
    }

    @Loggable(action = "REPAY_LOAN", category = "LOANS")
    public LoanDTO repayLoan(UUID loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        repaymentService.processPayment(loan, amount);
        return convertToDTO(loanRepository.save(loan));
    }

    // ========================================================================
    // 4. HELPERS
    // ========================================================================

    public LoanDTO approveLoan(UUID id) { officerApprove(id); return getLoanById(id); }
    public LoanDTO rejectLoan(UUID id) { secretaryFinalize(id, false, "Admin Rejected"); return getLoanById(id); }
    public LoanDTO disburseLoan(UUID id) { treasurerDisburse(id, "CASH-" + System.currentTimeMillis()); return getLoanById(id); }

    public void writeOffLoan(UUID id, String reason) {
        Loan loan = loanRepository.findById(id).orElseThrow();
        loan.setStatus(Loan.LoanStatus.WRITTEN_OFF);
        loanRepository.save(loan);
    }

    public LoanDTO restructureLoan(UUID id, Integer newDuration) {
        Loan loan = loanRepository.findById(id).orElseThrow();
        loan.setDuration(newDuration);
        repaymentService.generateRepaymentSchedule(loan, 0);
        return convertToDTO(loanRepository.save(loan));
    }

    public List<LoanDTO> getAllLoans() {
        return loanRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<LoanDTO> getLoansByMemberId(UUID memberId) {
        return loanRepository.findByMemberId(memberId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public LoanDTO getLoanById(UUID id) {
        return convertToDTO(loanRepository.findById(id).orElseThrow());
    }

    public BigDecimal getTotalDisbursedLoans() {
        return loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalOutstandingLoans() {
        return loanRepository.findAll().stream()
                .map(l -> l.getLoanBalance() != null ? l.getLoanBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalInterestCollected() {
        return BigDecimal.ZERO;
    }

    private LoanDTO convertToDTO(Loan loan) {
        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .memberId(loan.getMember().getId())
                .memberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .status(loan.getStatus().toString())
                .approvalDate(loan.getApprovalDate())
                .disbursementDate(loan.getDisbursementDate())
                .build();
    }

    public BigDecimal calculateMemberLoanLimit(Member member) {
        // 1. Get the multiplier from settings (Default to 3 if not set)
        double multiplier = systemSettingService.getDouble("LOAN_LIMIT_MULTIPLIER");
        if (multiplier <= 0) multiplier = 3.0;

        // 2. Calculate Max Potential: Total Savings * Multiplier
        BigDecimal maxPotential = member.getTotalSavings().multiply(BigDecimal.valueOf(multiplier));

        // 3. Calculate Current Debt: Sum of balances of all active loans
        List<Loan> loans = loanRepository.findByMemberId(member.getId());
        BigDecimal currentDebt = loans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED ||
                        l.getStatus() == Loan.LoanStatus.ACTIVE ||
                        l.getStatus() == Loan.LoanStatus.APPROVED)
                .map(Loan::getLoanBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Final Limit: Max Potential - Current Debt (Ensure it's not negative)
        return maxPotential.subtract(currentDebt).max(BigDecimal.ZERO);
    }
}