package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.ReferenceCodeService;
import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor.GuarantorStatus;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;
    private final GuarantorRepository guarantorRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final AccountingService accountingService;
    private final TransactionRepository transactionRepository;
    private final LoanLimitService loanLimitService;
    private final RepaymentScheduleService repaymentScheduleService;
    private final SystemSettingService systemSettingService;
    private final NotificationService notificationService;
    private final ReferenceCodeService referenceCodeService;

    // --- HELPER: SAFE MEMBER LOOKUP ---
    private Optional<Member> getMemberSafely(UUID userId) {
        return memberRepository.findByUserId(userId);
    }

    // --- PHASE 1: ELIGIBILITY & LIMITS ---

    public Map<String, Object> checkEligibility(UUID userId) {
        Optional<Member> memberOpt = getMemberSafely(userId);

        if (memberOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("eligible", false);
            errorResponse.put("reasons", List.of("Admin/User does not have a Member profile."));
            return errorResponse;
        }

        Member member = memberOpt.get();
        int requiredMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_MEMBERSHIP", "3"));
        BigDecimal requiredSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_FOR_LOAN", "5000"));
        int maxActiveLoans = Integer.parseInt(systemSettingService.getString("MAX_ACTIVE_LOANS", "1"));

        List<String> reasons = new ArrayList<>();
        BigDecimal currentSavings = calculateNewBalance(member);

        long activeLoanCount = loanRepository.countByMemberIdAndStatusIn(
                member.getId(),
                List.of(Loan.LoanStatus.ACTIVE, Loan.LoanStatus.IN_ARREARS)
        );

        if (activeLoanCount >= maxActiveLoans) {
            reasons.add("You have reached the maximum of " + maxActiveLoans + " active loan(s).");
        }

        if (currentSavings.compareTo(requiredSavings) < 0) {
            reasons.add("Minimum savings of KES " + requiredSavings + " required.");
        }

        long monthsActive = 0;
        if (member.getCreatedAt() != null) {
            monthsActive = java.time.temporal.ChronoUnit.MONTHS.between(member.getCreatedAt(), LocalDateTime.now());
        }

        if (monthsActive < requiredMonths) {
            reasons.add("Minimum membership of " + requiredMonths + " month(s) required.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("eligible", reasons.isEmpty());
        response.put("reasons", reasons);
        response.put("currentSavings", currentSavings);

        return response;
    }

    public List<Member> getEligibleGuarantors(UUID applicantId) {
        BigDecimal minSavings = new BigDecimal(systemSettingService.getString("MIN_SAVINGS_TO_GUARANTEE", "10000"));
        int minMonths = Integer.parseInt(systemSettingService.getString("MIN_MONTHS_TO_GUARANTEE", "6"));

        List<Member> candidates = memberRepository.findByStatus(Member.MemberStatus.ACTIVE).stream()
                .filter(m -> !m.getId().equals(applicantId))
                .collect(Collectors.toList());

        List<Member> eligible = new ArrayList<>();

        for (Member m : candidates) {
            BigDecimal savings = calculateNewBalance(m);

            if (savings.compareTo(minSavings) < 0) continue;

            long monthsActive = 0;
            if (m.getCreatedAt() != null) {
                monthsActive = java.time.temporal.ChronoUnit.MONTHS.between(m.getCreatedAt().toLocalDate(), LocalDate.now());
            }
            if (monthsActive < minMonths) continue;

            boolean hasDefaults = loanRepository.findByMemberId(m.getId()).stream()
                    .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED || l.getStatus() == Loan.LoanStatus.WRITTEN_OFF);

            if (hasDefaults) continue;

            eligible.add(m);
        }

        return eligible;
    }

    // --- PHASE 1: INITIATION (ORIGINAL SINGLE-STEP FLOW) ---

    public LoanDTO initiateWithFee(UUID userId, UUID productId, String userExternalRef, String paymentMethodStr) {
        Member member = getMemberSafely(userId)
                .orElseThrow(() -> new RuntimeException("Only registered Members can apply for loans."));

        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        BigDecimal fee = product.getProcessingFee() != null ? product.getProcessingFee() : BigDecimal.ZERO;

        // 1. Process Payment Immediately
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            String systemRef = referenceCodeService.generateReferenceCode();
            String sourceAccount = "1002"; // Default M-Pesa
            Transaction.PaymentMethod payMethod = Transaction.PaymentMethod.MPESA;

            if ("BANK".equalsIgnoreCase(paymentMethodStr)) {
                sourceAccount = "1010";
                payMethod = Transaction.PaymentMethod.BANK;
            } else if ("CASH".equalsIgnoreCase(paymentMethodStr)) {
                sourceAccount = "1001";
                payMethod = Transaction.PaymentMethod.CASH;
            }

            accountingService.postEvent(
                    "LOAN_PROCESSING_FEE",
                    "App Fee - " + member.getMemberNumber(),
                    systemRef,
                    fee,
                    sourceAccount,
                    null
            );

            BigDecimal currentTotalSavings = calculateNewBalance(member);
            Transaction feeTransaction = Transaction.builder()
                    .member(member)
                    .amount(fee)
                    .type(Transaction.TransactionType.PROCESSING_FEE)
                    .paymentMethod(payMethod)
                    .referenceCode(systemRef)
                    .externalReference(userExternalRef)
                    .description("Loan App Fee: " + product.getName())
                    .balanceAfter(currentTotalSavings)
                    .transactionDate(LocalDateTime.now())
                    .build();

            transactionRepository.save(feeTransaction);

            notificationService.notifyUser(
                    member.getId(),
                    "Payment Received",
                    "Loan Fee Received. Receipt: " + systemRef,
                    true,
                    false
            );
        }

        // 2. Create Loan in DRAFT status
        Loan loan = Loan.builder()
                .loanNumber("LN-" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(BigDecimal.ZERO)
                .status(Loan.LoanStatus.DRAFT) // Original Status
                .applicationDate(LocalDate.now())
                .applicationFeePaid(true)
                .gracePeriodWeeks(0)
                .votingOpen(false)
                .votesYes(0)
                .votesNo(0)
                .build();

        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO submitApplication(UUID loanId, BigDecimal amount, Integer duration) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.isApplicationFeePaid()) {
            throw new RuntimeException("Application fee must be paid before submitting details.");
        }

        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(loan.getMember());
        BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");
        if (amount.compareTo(availableLimit) > 0)
            throw new RuntimeException("Exceeds limit of KES " + availableLimit);

        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(amount,
                loan.getProduct().getInterestRate(), duration, Loan.DurationUnit.WEEKS);

        validateAbilityToPay(loan.getMember(), weeklyRepayment);

        String gracePeriodStr = systemSettingService.getString("LOAN_GRACE_PERIOD_WEEKS", "0");
        try {
            loan.setGracePeriodWeeks(Integer.parseInt(gracePeriodStr));
        } catch (NumberFormatException e) {
            loan.setGracePeriodWeeks(0);
        }

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setWeeklyRepaymentAmount(weeklyRepayment);

        // Lock details
        loan.setInterestRate(loan.getProduct().getInterestRate());

        // ✅ CORRECT: Do NOT calculate expectedRepaymentDate here. It is too early.
        // It will be calculated upon disbursement.

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);

        return convertToDTO(loanRepository.save(loan));
    }

    // --- PHASE 2: GUARANTORS ---

    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow(() -> new RuntimeException("Guarantor not found"));

        if (guarantor.getId().equals(loan.getMember().getId())) {
            throw new RuntimeException("Conflict: You cannot guarantee your own loan.");
        }

        boolean alreadyAdded = loan.getGuarantors().stream()
                .anyMatch(g -> g.getMember().getId().equals(guarantorMemberId));
        if (alreadyAdded) {
            throw new RuntimeException("This member is already a guarantor for this loan.");
        }

        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        g = guarantorRepository.save(g);

        try {
            String guarantorMessage = String.format(
                    "Hello %s, member %s has requested you to guarantee their loan (%s) for an amount of KES %s. Please log in to your dashboard to Accept or Decline.",
                    guarantor.getFirstName(),
                    loan.getMemberName(),
                    loan.getLoanNumber(),
                    amount
            );
            notificationService.notifyUser(guarantor.getId(), "Action Required: Guarantorship Request", guarantorMessage, true, false);
        } catch (Exception e) {
            log.error("Failed to send immediate notification to guarantor: {}", e.getMessage());
        }

        return convertToGuarantorDTO(g);
    }

    public void finalizeGuarantorRequests(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getGuarantors().isEmpty()) {
            throw new RuntimeException("At least one guarantor is required.");
        }

        int countSent = 0;
        for (Guarantor g : loan.getGuarantors()) {
            if (g.getStatus() == GuarantorStatus.PENDING) {
                String guarantorMessage = String.format(
                        "Reminder: Hello %s, member %s is still waiting for you to guarantee their loan (%s). Amount: KES %s.",
                        g.getMember().getFirstName(),
                        loan.getMemberName(),
                        loan.getLoanNumber(),
                        g.getGuaranteeAmount()
                );
                notificationService.notifyUser(g.getMember().getId(), "Reminder: Guarantorship Request", guarantorMessage, true, false);
                countSent++;
            }
        }

        if (countSent > 0) {
            notificationService.notifyUser(loan.getMember().getId(), "Guarantor Requests Resent", "We have resent notifications to " + countSent + " pending guarantors.", true, false);
        }

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        loanRepository.save(loan);
    }

    public void respondToGuarantorRequest(UUID userId, UUID requestId, String responseStatus) {
        Guarantor request = guarantorRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Guarantor request not found"));

        if (!request.getMember().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You are not the guarantor for this request.");
        }

        GuarantorStatus status;
        try {
            status = GuarantorStatus.valueOf(responseStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status. Use ACCEPTED or DECLINED.");
        }

        request.setStatus(status);
        request.setDateResponded(LocalDate.now());
        guarantorRepository.save(request);

        String message = "Your guarantor " + request.getMember().getFirstName() + " has " + status.toString().toLowerCase() + " your request.";
        notificationService.notifyUser(request.getLoan().getMember().getId(), "Guarantor Update", message, true, true);

        checkAndProgressLoan(request.getLoan());
    }

    private void checkAndProgressLoan(Loan loan) {
        List<Guarantor> allGuarantors = loan.getGuarantors();
        boolean allAccepted = allGuarantors.stream().allMatch(g -> g.getStatus() == GuarantorStatus.ACCEPTED);

        if (allAccepted) {
            loan.setStatus(Loan.LoanStatus.SUBMITTED);
            loan.setSubmissionDate(LocalDate.now());

            loanRepository.save(loan);
            notificationService.notifyUser(loan.getMember().getId(), "Loan Application Submitted", "Great news! All guarantors have accepted. Your application is now with the Loan Officer for review.", true, true);
        }
    }

    // --- PHASE 3: LOAN OFFICER REVIEW ---

    public List<LoanDTO> getPendingLoansForAdmin() {
        // Return ALL statuses relevant to Admin, Secretary, and Chairperson
        return loanRepository.findByStatusIn(List.of(
                        Loan.LoanStatus.SUBMITTED,
                        Loan.LoanStatus.LOAN_OFFICER_REVIEW,
                        Loan.LoanStatus.APPROVED,
                        Loan.LoanStatus.SECRETARY_TABLED,
                        Loan.LoanStatus.VOTING_OPEN,
                        Loan.LoanStatus.SECRETARY_DECISION, // ✅ Added for Chair's View
                        Loan.LoanStatus.TREASURER_DISBURSEMENT // ✅ Added so Treasurer sees approved loans
                )).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void reviewLoanApplication(UUID adminUserId, UUID loanId, String decision, String remarks) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SUBMITTED && loan.getStatus() != Loan.LoanStatus.LOAN_OFFICER_REVIEW) {
            throw new RuntimeException("This loan is not in the correct stage for review.");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {
            loan.setStatus(Loan.LoanStatus.APPROVED);
            loan.setApprovalDate(LocalDate.now());
            notificationService.notifyUser(loan.getMember().getId(), "Loan Approved!", "Your application has been approved and forwarded to the Secretary.", true, true);
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason(remarks);
            notificationService.notifyUser(loan.getMember().getId(), "Loan Application Rejected", "Reason: " + remarks, true, true);
        }

        loanRepository.save(loan);
    }

    // --- PHASE 3.5: GOVERNANCE (SECRETARY & CHAIR) ---

    // ✅ FIXED: Now takes LocalDateTime for exact scheduling
    public void tableLoanForMeeting(UUID loanId, LocalDateTime meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.APPROVED) {
            throw new RuntimeException("Only technically approved loans can be tabled for a meeting.");
        }

        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loan.setMeetingDate(meetingDate);
        loanRepository.save(loan);

        notificationService.notifyUser(loan.getMember().getId(), "Loan Tabled", "Your loan has been scheduled for the committee meeting on " + meetingDate.toLocalDate() + " at " + meetingDate.toLocalTime(), true, true);
    }

    public void startVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_TABLED) {
            throw new RuntimeException("This loan has not been tabled for a meeting yet.");
        }

        // ✅ FIXED: Enforce Time Constraint
        if (loan.getMeetingDate() != null && LocalDateTime.now().isBefore(loan.getMeetingDate())) {
            throw new RuntimeException("Cannot start voting yet. Scheduled for " + loan.getMeetingDate().toString().replace("T", " "));
        }

        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        loan.setVotingOpen(true);
        loanRepository.save(loan);

        log.info("Voting opened for loan {}", loan.getLoanNumber());
    }

    // ✅ NEW: Member Casting Vote (Before Secretary Finalizes)
    public void castVote(UUID loanId, UUID userId, boolean voteYes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.VOTING_OPEN) {
            throw new RuntimeException("Voting is not open for this loan.");
        }

        Member voter = getMemberSafely(userId).orElseThrow(() -> new RuntimeException("Voter must be a member"));

        // Prevent voting on own loan
        if (loan.getMember().getId().equals(voter.getId())) {
            throw new RuntimeException("You cannot vote on your own loan.");
        }

        // Prevent Double Voting
        if (loan.getVotedUserIds() != null && loan.getVotedUserIds().contains(userId)) {
            throw new RuntimeException("You have already voted.");
        }

        // Initialize lists/counters if null (Safety)
        if (loan.getVotesYes() == null) loan.setVotesYes(0);
        if (loan.getVotesNo() == null) loan.setVotesNo(0);
        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new ArrayList<>());

        // Record Vote
        if (voteYes) {
            loan.setVotesYes(loan.getVotesYes() + 1);
        } else {
            loan.setVotesNo(loan.getVotesNo() + 1);
        }

        loan.getVotedUserIds().add(userId);
        loanRepository.save(loan);
    }

    // ✅ NEW: Get active votes for member (Filters own loans)
    public List<LoanDTO> getActiveVotesForMember(UUID userId) {
        List<Loan> openLoans = loanRepository.findByStatus(Loan.LoanStatus.VOTING_OPEN);
        Member voter = memberRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Member not found"));

        return openLoans.stream()
                // STRICT EXCLUSION: Applicant cannot see their own loan in voting list
                .filter(loan -> !loan.getMember().getId().equals(voter.getId()))
                // EXCLUSION: Remove already voted
                .filter(loan -> loan.getVotedUserIds() == null || !loan.getVotedUserIds().contains(userId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ NEW METHOD: Secretary Finalizes Vote -> Moves to Chair
    public void closeVoting(UUID loanId, boolean approved, String minutes) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.VOTING_OPEN) {
            throw new RuntimeException("Voting is not open for this loan.");
        }

        loan.setVotingOpen(false);
        loan.setSecretaryComments(minutes); // Capture minutes/verdict

        if (approved) {
            // ✅ CHANGED: Moves to SECRETARY_DECISION (Chair Approval Required)
            loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
            notificationService.notifyUser(loan.getMember().getId(), "Committee Approved", "Your loan passed the committee vote. Pending final sign-off by Chairperson.", true, true);
        } else {
            loan.setStatus(Loan.LoanStatus.REJECTED);
            loan.setRejectionReason("Committee Rejected: " + minutes);
            notificationService.notifyUser(loan.getMember().getId(), "Loan Rejected", "The committee voted against your loan.", true, true);
        }
        loanRepository.save(loan);
    }

    // ✅ NEW METHOD: Chairperson Final Approval -> Moves to Treasurer
    public void chairpersonFinalApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_DECISION) {
            throw new RuntimeException("Loan is not pending Chairperson approval.");
        }

        // ✅ MOVES TO TREASURER
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loanRepository.save(loan);

        notificationService.notifyUser(loan.getMember().getId(), "Final Approval", "Chairperson has signed off. Your loan is now in the disbursement queue.", true, true);
    }

    // --- PHASE 4: DISBURSEMENT ---

    public void disburseLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        // ✅ STRICT LOCK: Ensure Chair Passed it
        if (loan.getStatus() != Loan.LoanStatus.TREASURER_DISBURSEMENT) {
            throw new RuntimeException("Loan not ready for disbursement. Chairperson approval required.");
        }

        String ref = referenceCodeService.generateReferenceCode();
        BigDecimal amount = loan.getPrincipalAmount();

        // 1. Accounting
        accountingService.postEvent(
                "LOAN_DISBURSEMENT",
                "Disbursement: " + loan.getLoanNumber(),
                ref,
                amount,
                "1002",
                "2001"
        );

        // 2. Transaction
        Transaction transaction = Transaction.builder()
                .member(loan.getMember())
                .amount(amount)
                .type(Transaction.TransactionType.LOAN_DISBURSEMENT)
                .referenceCode(ref)
                .description("Disbursement for Loan: " + loan.getLoanNumber())
                .transactionDate(LocalDateTime.now())
                .balanceAfter(calculateNewBalance(loan.getMember()))
                .build();
        transactionRepository.save(transaction);

        // 3. Update Loan Status & Calculate Dates
        loan.setStatus(Loan.LoanStatus.DISBURSED);

        LocalDate today = LocalDate.now();
        loan.setDisbursementDate(today);
        loan.setLoanBalance(amount);

        // ✅ FIXED: Calculate Repayment Date Here (Today + Grace + Duration)
        int graceWeeks = loan.getGracePeriodWeeks() != null ? loan.getGracePeriodWeeks() : 0;
        int durationWeeks = loan.getDuration() != null ? loan.getDuration() : 0;

        LocalDate startDate = today.plusWeeks(graceWeeks);
        LocalDate endDate = startDate.plusWeeks(durationWeeks);

        loan.setExpectedRepaymentDate(endDate);

        loanRepository.save(loan);

        notificationService.notifyUser(loan.getMember().getId(), "Funds Disbursed", "KES " + amount + " has been deposited to your savings account.", true, true);
    }

    // --- HELPERS & GETTERS ---

    private BigDecimal calculateNewBalance(Member member) {
        return savingsAccountRepository.findByMember_Id(member.getId())
                .stream()
                .map(SavingsAccount::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<GuarantorDTO> getGuarantorsByLoan(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        return guarantorRepository.findByLoan(loan).stream()
                .map(this::convertToGuarantorDTO)
                .collect(Collectors.toList());
    }

    public List<GuarantorDTO> getGuarantorRequests(UUID userId) {
        Optional<Member> memberOpt = getMemberSafely(userId);
        if (memberOpt.isEmpty()) return Collections.emptyList();

        return guarantorRepository.findByMemberAndStatus(memberOpt.get(), Guarantor.GuarantorStatus.PENDING)
                .stream()
                .map(this::convertToGuarantorDTO)
                .collect(Collectors.toList());
    }

    public List<LoanDTO> getLoansByMember(UUID userId) {
        return getMemberSafely(userId)
                .map(member -> loanRepository.findByMemberId(member.getId()).stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public LoanDTO getLoanById(UUID loanId) {
        return loanRepository.findById(loanId).map(this::convertToDTO).orElseThrow();
    }

    private void validateAbilityToPay(Member member, BigDecimal weeklyRepayment) {
        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
            BigDecimal monthlyRepayment = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
            String maxRatioStr = systemSettingService.getString("MAX_DEBT_RATIO", "0.66");
            double maxRatio = Double.parseDouble(maxRatioStr);
            if (monthlyRepayment.compareTo(emp.getNetMonthlyIncome().multiply(BigDecimal.valueOf(maxRatio))) > 0) {
                throw new RuntimeException("Repayment too high for income levels.");
            }
        }
    }

    private LoanDTO convertToDTO(Loan loan) {
        BigDecimal totalSavings = calculateNewBalance(loan.getMember());
        BigDecimal netIncome = BigDecimal.ZERO;
        if (loan.getMember().getEmploymentDetails() != null && loan.getMember().getEmploymentDetails().getNetMonthlyIncome() != null) {
            netIncome = loan.getMember().getEmploymentDetails().getNetMonthlyIncome();
        }

        return LoanDTO.builder()
                .id(loan.getId())
                .loanNumber(loan.getLoanNumber())
                .status(loan.getStatus().toString())
                .memberName(loan.getMemberName())
                .principalAmount(loan.getPrincipalAmount())
                .loanBalance(loan.getLoanBalance())
                .expectedRepaymentDate(loan.getExpectedRepaymentDate() != null ? loan.getExpectedRepaymentDate().toString() : null)
                // ✅ ADDED: Pass meeting date to DTO
                .meetingDate(loan.getMeetingDate() != null ? loan.getMeetingDate().toString() : null)
                .approvalDate(loan.getApprovalDate())
                .memberSavings(totalSavings)
                .memberNetIncome(netIncome)
                // ✅ ADDED: Votes
                .votesYes(loan.getVotesYes() != null ? loan.getVotesYes() : 0)
                .votesNo(loan.getVotesNo() != null ? loan.getVotesNo() : 0)
                .memberId(loan.getMember().getId())
                .build();
    }

    private GuarantorDTO convertToGuarantorDTO(Guarantor g) {
        if (g.getLoan() == null) {
            return GuarantorDTO.builder()
                    .id(g.getId())
                    .memberId(g.getMember().getId())
                    .loanId(null)
                    .loanNumber("N/A")
                    .applicantName("Unknown")
                    .memberName(g.getMember().getFirstName())
                    .guaranteeAmount(g.getGuaranteeAmount())
                    .status(g.getStatus() != null ? g.getStatus().toString() : "UNKNOWN")
                    .build();
        }

        return GuarantorDTO.builder()
                .id(g.getId())
                .memberId(g.getMember().getId())
                .loanId(g.getLoan().getId())
                .loanNumber(g.getLoan().getLoanNumber())
                .applicantName(g.getLoan().getMemberName())
                .memberName(g.getMember().getFirstName() + " " + g.getMember().getLastName())
                .guaranteeAmount(g.getGuaranteeAmount())
                .status(g.getStatus().toString())
                .build();
    }
}