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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final NotificationService notificationService;
    private final LoanLimitService loanLimitService;
    private final UserRepository userRepository;

    // ========================================================================
    // 1. MEMBER: APPLICATION PHASE
    // ========================================================================

    @Loggable(action = "INITIATE_APPLICATION", category = "LOANS")
    public LoanDTO initiateApplication(UUID memberId, UUID productId, BigDecimal amount, Integer duration, String unit) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        if (amount.compareTo(product.getMaxLimit()) > 0)
            throw new RuntimeException("Amount exceeds product limit of " + product.getMaxLimit());

        BigDecimal memberLimit = loanLimitService.calculateMemberLoanLimit(member);
        if (amount.compareTo(memberLimit) > 0) {
            throw new RuntimeException("Amount exceeds your qualifying limit of KES " + memberLimit);
        }

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

    @Loggable(action = "UPDATE_DRAFT", category = "LOANS")
    public LoanDTO updateApplication(UUID loanId, BigDecimal amount, Integer duration, String unit) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.DRAFT) {
            throw new RuntimeException("Cannot edit application that is already submitted.");
        }

        BigDecimal memberLimit = loanLimitService.calculateMemberLoanLimit(loan.getMember());
        if (amount.compareTo(memberLimit) > 0) {
            throw new RuntimeException("New amount exceeds limit of KES " + memberLimit);
        }

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setDurationUnit(Loan.DurationUnit.valueOf(unit));

        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO applyForLoan(UUID memberId, UUID productId, BigDecimal amount, Integer duration) {
        return initiateApplication(memberId, productId, amount, duration, "MONTHS");
    }

    @Loggable(action = "DELETE_APPLICATION", category = "LOANS")
    public void deleteApplication(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        List<Loan.LoanStatus> deletableStatuses = List.of(
                Loan.LoanStatus.DRAFT,
                Loan.LoanStatus.GUARANTORS_PENDING,
                Loan.LoanStatus.GUARANTORS_APPROVED,
                Loan.LoanStatus.APPLICATION_FEE_PENDING
        );

        if (!deletableStatuses.contains(loan.getStatus())) {
            throw new RuntimeException("Cannot delete loan application in status: " + loan.getStatus());
        }

        if (loan.isApplicationFeePaid()) {
            throw new RuntimeException("Cannot delete application where fee is already paid.");
        }

        loanRepository.delete(loan);
    }

    public List<GuarantorDTO> getLoanGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        return loan.getGuarantors().stream()
                .map(g -> GuarantorDTO.builder()
                        .id(g.getId())
                        .memberId(g.getMember().getId())
                        .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                        .guaranteeAmount(g.getGuaranteeAmount())
                        .status(g.getStatus().toString())
                        .build())
                .collect(Collectors.toList());
    }

    @Loggable(action = "SUBMIT_TO_GUARANTORS", category = "LOANS")
    public void submitToGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if(loan.getGuarantors() == null || loan.getGuarantors().isEmpty())
            throw new RuntimeException("At least one guarantor required");

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        loanRepository.save(loan);

        notificationService.createNotification(
                loan.getMember().getUser(),
                "Guarantor Requests Sent",
                "Your loan application has been sent to the selected guarantors for approval.",
                Notification.NotificationType.INFO
        );

        for (Guarantor g : loan.getGuarantors()) {
            if (g.getMember().getUser() != null) {
                String msg = String.format("Request from %s %s: Please guarantee loan %s for KES %s. Your liability: KES %s",
                        loan.getMember().getFirstName(), loan.getMember().getLastName(),
                        loan.getLoanNumber(), loan.getPrincipalAmount(), g.getGuaranteeAmount());

                notificationService.createNotification(
                        g.getMember().getUser(),
                        "Guarantorship Request",
                        msg,
                        Notification.NotificationType.ACTION_REQUIRED
                );
            }
        }
    }

    @Loggable(action = "ADD_GUARANTOR", category = "LOANS")
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();

        if(guarantor.getId().equals(loan.getMember().getId()))
            throw new RuntimeException("Cannot guarantee self.");

        if(guarantor.getTotalSavings().compareTo(amount) < 0){
            throw new RuntimeException("Guarantor " + guarantor.getFirstName() + " does not have enough savings (KES " + guarantor.getTotalSavings() + ") to cover KES " + amount);
        }

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

    public List<Map<String, Object>> getPendingGuarantorRequests(UUID memberId) {
        return guarantorRepository.findByMemberIdAndStatus(memberId, Guarantor.GuarantorStatus.PENDING)
                .stream()
                .map(g -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("requestId", g.getId());
                    map.put("loanAmount", g.getLoan().getPrincipalAmount());
                    map.put("guaranteeAmount", g.getGuaranteeAmount());
                    map.put("applicantName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName());
                    map.put("loanProduct", g.getLoan().getProduct().getName());
                    map.put("dateRequested", g.getDateRequestSent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Loggable(action = "RESPOND_GUARANTORSHIP", category = "LOANS")
    public void respondToGuarantorship(UUID guarantorId, boolean accepted) {
        Guarantor g = guarantorRepository.findById(guarantorId).orElseThrow(() -> new RuntimeException("Request not found"));

        g.setStatus(accepted ? Guarantor.GuarantorStatus.ACCEPTED : Guarantor.GuarantorStatus.DECLINED);
        g.setDateResponded(LocalDate.now());
        guarantorRepository.save(g);

        Notification.NotificationType type = accepted ? Notification.NotificationType.SUCCESS : Notification.NotificationType.WARNING;
        String statusText = accepted ? "Accepted" : "Declined";

        notificationService.createNotification(
                g.getLoan().getMember().getUser(),
                "Guarantor Responded",
                String.format("%s %s has %s your guarantorship request.", g.getMember().getFirstName(), g.getMember().getLastName(), statusText),
                type
        );

        Loan loan = g.getLoan();
        long pending = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.PENDING);
        long declined = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.DECLINED);

        if (pending == 0) {
            if (declined == 0) {
                loan.setStatus(Loan.LoanStatus.GUARANTORS_APPROVED);
                notificationService.createNotification(loan.getMember().getUser(), "Guarantors Approved", "All guarantors have accepted! You can now pay the application fee.", Notification.NotificationType.SUCCESS);
            } else {
                notificationService.createNotification(loan.getMember().getUser(), "Guarantor Update", "All guarantors responded, but some declined. Please review.", Notification.NotificationType.WARNING);
            }
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
    // 3. WORKFLOW & APPROVALS (OFFICER -> SECRETARY -> CHAIRPERSON -> ADMIN)
    // ========================================================================

    @Loggable(action = "OFFICER_REVIEW", category = "LOANS")
    public LoanDTO officerReview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new RuntimeException("Loan must be in SUBMITTED status to start review.");
        }
        loan.setStatus(Loan.LoanStatus.LOAN_OFFICER_REVIEW);
        return convertToDTO(loanRepository.save(loan));
    }

    @Loggable(action = "OFFICER_APPROVE", category = "LOANS")
    public void officerApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (loan.getStatus() != Loan.LoanStatus.LOAN_OFFICER_REVIEW && loan.getStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new RuntimeException("Loan must be reviewed before approval.");
        }
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loanRepository.save(loan);

        // Notify Secretaries
        List<User> secretaries = userRepository.findByRole(User.Role.SECRETARY);
        for (User secretary : secretaries) {
            notificationService.createNotification(
                    secretary,
                    "New Loan Tabled",
                    String.format("Loan %s for %s has been approved by the Loan Officer and is ready for tabling.",
                            loan.getLoanNumber(), loan.getMember().getFirstName()),
                    Notification.NotificationType.ACTION_REQUIRED
            );
        }
    }

    // ✅ FIXED: ADDED NOTIFICATION LOGIC BACK TO TABLE LOAN
    @Loggable(action = "TABLE_LOAN", category = "LOANS")
    public void tableLoan(UUID loanId, LocalDate meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_TABLED) {
            throw new RuntimeException("Loan is not ready for tabling.");
        }

        loan.setMeetingDate(meetingDate);
        loan.setStatus(Loan.LoanStatus.ON_AGENDA);
        loanRepository.save(loan);

        // 1. Notify Applicant
        if (loan.getMember().getUser() != null) {
            notificationService.createNotification(
                    loan.getMember().getUser(),
                    "Application Tabled",
                    "Your loan application has been added to the agenda for the committee meeting on " + meetingDate,
                    Notification.NotificationType.INFO
            );
        }

        // 2. Notify Chairperson & Treasurer
        List<User.Role> committeeRoles = List.of(User.Role.CHAIRPERSON, User.Role.TREASURER);

        for (User.Role role : committeeRoles) {
            List<User> officials = userRepository.findByRole(role);
            if (officials.isEmpty()) {
                System.out.println("⚠️ Warning: No users found with role " + role + ". Notification skipped.");
            }
            for (User official : officials) {
                notificationService.createNotification(
                        official,
                        "New Agenda Item",
                        String.format("Loan Ref %s (Applicant: %s) has been tabled for the meeting on %s.",
                                loan.getLoanNumber(), loan.getMember().getFirstName(), meetingDate),
                        Notification.NotificationType.ACTION_REQUIRED
                );
            }
        }
    }

    @Loggable(action = "OPEN_VOTING", category = "LOANS")
    public void openVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ON_AGENDA) {
            throw new RuntimeException("Loan must be tabled on the agenda before voting can start.");
        }

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

        BigDecimal currentLiquidity = accountingService.getAccountBalance("1001");
        if (currentLiquidity.compareTo(loan.getPrincipalAmount()) < 0) {
            throw new RuntimeException("Disbursement Failed: Insufficient Sacco liquidity. Available: KES " + currentLiquidity);
        }

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
    // 5. HELPERS
    // ========================================================================

    public LoanDTO approveLoan(UUID id) { officerApprove(id); return getLoanById(id); }
    public LoanDTO rejectLoan(UUID id) { secretaryFinalize(id, false, "Rejected"); return getLoanById(id); }
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
                .productName(loan.getProduct().getName())
                .processingFee(loan.getProduct().getProcessingFee())
                .memberSavings(loan.getMember().getTotalSavings())
                .build();
    }
}