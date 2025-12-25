package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final GuarantorRepository guarantorRepository;
    private final AccountingService accountingService;
    private final LoanLimitService loanLimitService;
    private final RepaymentScheduleService repaymentScheduleService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // --- STEP 1: Fee-First Entry ---
    public LoanDTO initiateWithFee(UUID memberId, UUID productId, String referenceCode) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal fee = product.getProcessingFee() != null ? product.getProcessingFee() : BigDecimal.ZERO;
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            accountingService.postEvent("LOAN_PROCESSING_FEE", "App Fee - " + member.getMemberNumber(), referenceCode,
                    fee);
        }

        Loan loan = Loan.builder()
                .loanNumber("LN-DR-" + System.currentTimeMillis())
                .member(member).product(product).principalAmount(BigDecimal.ZERO)
                .status(Loan.LoanStatus.DRAFT).applicationDate(LocalDate.now()).applicationFeePaid(true).build();

        return convertToDTO(loanRepository.save(loan));
    }

    // --- STEP 2: Application Details ---
    public LoanDTO submitApplication(UUID loanId, BigDecimal amount, Integer duration) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.DRAFT || !loan.isApplicationFeePaid()) {
            throw new RuntimeException("Application fee must be paid before submitting details.");
        }

        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(loan.getMember());
        BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");
        if (amount.compareTo(availableLimit) > 0)
            throw new RuntimeException("Exceeds limit of KES " + availableLimit);

        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(amount,
                loan.getProduct().getInterestRate(), duration, Loan.DurationUnit.MONTHS);
        validateAbilityToPay(loan.getMember(), weeklyRepayment);

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        return convertToDTO(loanRepository.save(loan));
    }

    private void validateAbilityToPay(Member member, BigDecimal weeklyRepayment) {
        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
            BigDecimal monthlyRepayment = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
            double maxRatio = systemSettingService.getDouble("MAX_DEBT_RATIO", 0.66);
            if (monthlyRepayment.compareTo(emp.getNetMonthlyIncome().multiply(BigDecimal.valueOf(maxRatio))) > 0) {
                throw new RuntimeException("Repayment too high for income.");
            }
        }
    }

    // --- STEP 3: Guarantors ---
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();
        if (guarantor.getId().equals(loan.getMember().getId()))
            throw new RuntimeException("Conflict: Self-guarantee.");

        Guarantor g = Guarantor.builder().loan(loan).member(guarantor).guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING).dateRequestSent(LocalDate.now()).build();

        notificationService.notifyUser(guarantor.getId(), "Guarantorship Request",
                "Action required for loan " + loan.getLoanNumber(), true, false);
        return convertToGuarantorDTO(guarantorRepository.save(g));
    }

    // --- STEP 4: Officer Review ---
    public LoanDTO submitToLoanOfficer(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.LOAN_OFFICER_REVIEW);
        return convertToDTO(loanRepository.save(loan));
    }

    public void officerApprove(UUID loanId, String reviewComments) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loan.setSecretaryComments("Officer Review: " + reviewComments);
        loanRepository.save(loan);
    }

    // --- STEP 5: Secretary Tabling ---
    public void tableLoanOnAgenda(UUID loanId, LocalDate meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_TABLED)
            throw new RuntimeException("Pass technical review first.");
        loan.setMeetingDate(meetingDate);
        loan.setStatus(Loan.LoanStatus.ON_AGENDA);
        loanRepository.save(loan);
    }

    /**
     * --- STEP 6: Voting Floor ---
     * Strictly triggered by the Chairperson via the ChairpersonDashboard.
     */
    public void openVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Guard: Only loans assigned to an agenda by the Secretary can be opened for
        // voting
        if (loan.getStatus() != Loan.LoanStatus.ON_AGENDA) {
            throw new RuntimeException("This loan is not yet on the meeting agenda.");
        }

        loan.setVotingOpen(true);
        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);

        // Initialize/Reset voting tally to ensure a clean start
        loan.setVotesYes(0);
        loan.setVotesNo(0);
        loan.setVotedUserIds(new ArrayList<>());

        loanRepository.save(loan);

        // Notify all members that a new vote is live
        notificationService.notifyAll("Live Voting Started",
                "The Chairperson has opened the floor for Loan " + loan.getLoanNumber() + ". Please cast your vote.");

        log.info("Chairperson opened voting for Loan #{}", loan.getLoanNumber());
    }

    public void castVote(UUID loanId, UUID userId, boolean voteYes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (!loan.isVotingOpen())
            throw new RuntimeException("Voting floor is closed.");
        if (loan.getVotedUserIds().contains(userId))
            throw new RuntimeException("You already voted.");

        if (voteYes)
            loan.setVotesYes(loan.getVotesYes() + 1);
        else
            loan.setVotesNo(loan.getVotesNo() + 1);

        loan.getVotedUserIds().add(userId);
        loanRepository.save(loan);
    }

    // --- STEP 7: Decision ---
    public void finalizeLoanDecision(UUID loanId, Boolean manualApproved, String comments) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        String method = systemSettingService.getString("LOAN_VOTING_METHOD", "AUTOMATIC");
        boolean isApproved = "MANUAL".equalsIgnoreCase(method) ? manualApproved
                : (loan.getVotesYes() > loan.getVotesNo());

        loan.setStatus(isApproved ? Loan.LoanStatus.TREASURER_DISBURSEMENT : Loan.LoanStatus.REJECTED);
        loan.setSecretaryComments(comments);
        loan.setVotingOpen(false);
        loanRepository.save(loan);
        notifyOutcome(loan, isApproved);
    }

    private void notifyOutcome(Loan loan, boolean approved) {
        String msg = approved ? "Loan approved. Moving to disbursement." : "Loan application rejected.";
        notificationService.notifyUser(loan.getMember().getId(), "Decision", msg, true, false);
    }

    private LoanDTO convertToDTO(Loan loan) {
        return LoanDTO.builder().id(loan.getId()).loanNumber(loan.getLoanNumber()).status(loan.getStatus().toString())
                .memberName(loan.getMemberName()).principalAmount(loan.getPrincipalAmount()).build();
    }

    private GuarantorDTO convertToGuarantorDTO(Guarantor g) {
        return GuarantorDTO.builder().id(g.getId())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .guaranteeAmount(g.getGuaranteeAmount()).status(g.getStatus().toString()).build();
    }

    /**
     * ✅ STEP 8: Treasurer Disbursement
     * Location: LoanService.java
     */
    public LoanDTO disburseLoan(UUID loanId, String transactionReference) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.TREASURER_DISBURSEMENT) {
            throw new RuntimeException("Loan is not cleared for disbursement.");
        }

        // 1. Accounting: Post the Disbursement
        // Debit: Loan Portfolio (Asset), Credit: Bank/M-Pesa (Asset)
        accountingService.postEvent(
                "LOAN_DISBURSEMENT",
                "Disbursement for " + loan.getLoanNumber(),
                transactionReference,
                loan.getPrincipalAmount());

        // 2. Update Loan State
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursementDate(LocalDate.now());
        loan.setLoanBalance(loan.getPrincipalAmount()); // Initial balance is the principal
        loan.setCheckNumber(transactionReference);

        Loan activeLoan = loanRepository.save(loan);

        // 3. Generate Repayment Schedule
        // This creates the list of expected weekly payments in the LoanRepayment table
        repaymentScheduleService.generateSchedule(activeLoan);

        log.info("Loan {} disbursed successfully via ref: {}", loan.getLoanNumber(), transactionReference);

        return convertToDTO(activeLoan);
    }
    /**
 * ✅ Added to resolve LoanController compilation errors
 */
public List<LoanDTO> getLoansByMember(UUID memberId) {
    return loanRepository.findByMemberId(memberId).stream()
            .map(this::convertToDTO)
            .toList();
}

/**
 * ✅ Added to resolve LoanController compilation errors
 */
public LoanDTO getLoanById(UUID loanId) {
    return loanRepository.findById(loanId)
            .map(this::convertToDTO)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
}
}