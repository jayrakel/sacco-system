package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.GuarantorRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.notification.domain.entity.Notification;

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
    private final RepaymentScheduleService repaymentScheduleService;

    // ========================================================================
    // 1. MEMBER: APPLICATION PHASE
    // ========================================================================

    /**
     * Check if a member is eligible to apply for a loan based on system thresholds
     */
    public Map<String, Object> checkLoanEligibility(Member member) {
        Map<String, Object> result = new HashMap<>();
        List<String> failureReasons = new ArrayList<>();
        boolean eligible = true;

        // Get system threshold settings
        BigDecimal minSavings = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SAVINGS_FOR_LOAN"));
        int minMonths = (int) systemSettingService.getDouble("MIN_MONTHS_MEMBERSHIP");
        BigDecimal minShareCapital = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SHARE_CAPITAL"));

        // Check 1: Minimum Savings
        BigDecimal currentSavings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
        if (currentSavings.compareTo(minSavings) < 0) {
            eligible = false;
            failureReasons.add("Insufficient savings. Required: KES " + minSavings.toPlainString() +
                ", Current: KES " + currentSavings.toPlainString());
        }

        // Check 2: Membership Duration
        if (member.getCreatedAt() != null) {
            long monthsMember = java.time.temporal.ChronoUnit.MONTHS.between(
                member.getCreatedAt().toLocalDate(),
                LocalDate.now()
            );
            if (monthsMember < minMonths) {
                eligible = false;
                failureReasons.add("Membership too recent. Required: " + minMonths +
                    " months, Current: " + monthsMember + " months");
            }
        }

        // Check 3: Share Capital (if applicable)
        BigDecimal currentShareCapital = member.getTotalShares() != null ? member.getTotalShares() : BigDecimal.ZERO;
        if (minShareCapital.compareTo(BigDecimal.ZERO) > 0 && currentShareCapital.compareTo(minShareCapital) < 0) {
            eligible = false;
            failureReasons.add("Insufficient share capital. Required: KES " + minShareCapital.toPlainString() +
                ", Current: KES " + currentShareCapital.toPlainString());
        }

        // Check 4: Member must be active
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            eligible = false;
            failureReasons.add("Member account is not active. Current status: " + member.getStatus());
        }

        // Build response
        result.put("success", true);
        result.put("eligible", eligible);
        result.put("memberName", member.getFirstName() + " " + member.getLastName());
        result.put("memberNumber", member.getMemberNumber());
        result.put("currentSavings", currentSavings);
        result.put("currentShareCapital", currentShareCapital);
        result.put("requiredSavings", minSavings);
        result.put("requiredMonths", minMonths);
        result.put("requiredShareCapital", minShareCapital);

        if (!eligible) {
            result.put("reasons", failureReasons);
            result.put("message", "You do not meet the loan eligibility requirements");
        } else {
            result.put("message", "You are eligible to apply for a loan");
            result.put("maxLoanAmount", loanLimitService.calculateMemberLoanLimit(member));
        }

        return result;
    }

    /**
     * DEPRECATED: No longer used - fee payment moved to after guarantor approval
     * Check if member has already paid application fee but hasn't completed application
     */
    @Deprecated
    public Map<String, Object> checkApplicationFeeStatus(Member member) {
        Map<String, Object> result = new HashMap<>();
        result.put("feePaid", false);
        result.put("hasDraft", false);
        result.put("message", "Fee payment is now done after guarantor approval");
        return result;
    }

    /**
     * DEPRECATED: No longer used - fee payment moved to after guarantor approval
     * Pay application fee and create a draft loan marked as FEE_PAID
     */
    @Deprecated
    @Transactional
    public LoanDTO payApplicationFeeAndCreateDraft(Member member, String referenceCode) {
        throw new RuntimeException("This method is deprecated. Fee payment is now done after guarantor approval.");
    }

    /**
     * Check if a member is eligible to be a guarantor
     */
    public Map<String, Object> checkGuarantorEligibility(Member member, BigDecimal guaranteeAmount) {
        Map<String, Object> result = new HashMap<>();
        List<String> failureReasons = new ArrayList<>();
        boolean eligible = true;

        // Get system threshold settings
        BigDecimal minSavings = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SAVINGS_TO_GUARANTEE"));
        int minMonths = (int) systemSettingService.getDouble("MIN_MONTHS_TO_GUARANTEE");
        double maxGuarantorRatio = systemSettingService.getDouble("MAX_GUARANTOR_LIMIT_RATIO");

        // Check 1: Minimum Savings to Guarantee
        BigDecimal currentSavings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
        if (currentSavings.compareTo(minSavings) < 0) {
            eligible = false;
            failureReasons.add("Insufficient savings to guarantee. Required: KES " + minSavings.toPlainString() +
                ", Current: KES " + currentSavings.toPlainString());
        }

        // Check 2: Membership Duration
        if (member.getCreatedAt() != null) {
            long monthsMember = java.time.temporal.ChronoUnit.MONTHS.between(
                member.getCreatedAt().toLocalDate(),
                LocalDate.now()
            );
            if (monthsMember < minMonths) {
                eligible = false;
                failureReasons.add("Membership too recent to guarantee. Required: " + minMonths +
                    " months, Current: " + monthsMember + " months");
            }
        }

        // Check 3: Guarantee Amount vs Savings
        if (guaranteeAmount != null && currentSavings.compareTo(guaranteeAmount) < 0) {
            eligible = false;
            failureReasons.add("Cannot guarantee KES " + guaranteeAmount.toPlainString() +
                " with only KES " + currentSavings.toPlainString() + " in savings");
        }

        // Check 4: Total Outstanding Guarantees
        // Calculate current exposure from existing guarantor commitments
        // Include BOTH ACCEPTED and PENDING (since pending are still commitments)
        BigDecimal currentGuarantorExposure = guarantorRepository.findByMemberId(member.getId()).stream()
            .filter(g -> g.getStatus() == Guarantor.GuarantorStatus.ACCEPTED ||
                        g.getStatus() == Guarantor.GuarantorStatus.PENDING)
            .map(Guarantor::getGuaranteeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxGuarantorLimit = currentSavings.multiply(BigDecimal.valueOf(maxGuarantorRatio));
        BigDecimal availableToGuarantee = maxGuarantorLimit.subtract(currentGuarantorExposure);

        if (guaranteeAmount != null && guaranteeAmount.compareTo(availableToGuarantee) > 0) {
            eligible = false;
            failureReasons.add("Exceeds guarantor limit. Available to guarantee: KES " +
                availableToGuarantee.toPlainString() + ", Requested: KES " + guaranteeAmount.toPlainString());
        }

        // Check 5: Member must be active
        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            eligible = false;
            failureReasons.add("Member account is not active. Current status: " + member.getStatus());
        }

        // Check 6: Cannot have active loan default
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        boolean hasDefault = memberLoans.stream()
            .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED);

        if (hasDefault) {
            eligible = false;
            failureReasons.add("Cannot guarantee while having defaulted loans");
        }

        // Build response
        result.put("success", true);
        result.put("eligible", eligible);
        result.put("memberName", member.getFirstName() + " " + member.getLastName());
        result.put("memberNumber", member.getMemberNumber());
        result.put("currentSavings", currentSavings);
        result.put("currentGuarantorExposure", currentGuarantorExposure);
        result.put("availableToGuarantee", availableToGuarantee);
        result.put("requiredSavings", minSavings);
        result.put("requiredMonths", minMonths);

        if (!eligible) {
            result.put("reasons", failureReasons);
            result.put("message", "This member cannot be a guarantor");
        } else {
            result.put("message", "This member is eligible to be a guarantor");
        }

        return result;
    }

    public LoanDTO initiateApplication(UUID memberId, UUID productId, BigDecimal amount, Integer duration, String unit) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        // Check 1: Product limit
        if (amount.compareTo(product.getMaxLimit()) > 0)
            throw new RuntimeException("Amount exceeds product limit of " + product.getMaxLimit());

        // Check 2: STRICT member limit (includes pending disbursements)
        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(member);
        BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");

        if (amount.compareTo(availableLimit) > 0) {
            // Build detailed error message
            StringBuilder errorMsg = new StringBuilder("Amount exceeds your available limit. ");
            errorMsg.append("Available: KES ").append(availableLimit).append(". ");

            BigDecimal pendingDisbursement = (BigDecimal) limitDetails.get("pendingDisbursement");
            BigDecimal underReview = (BigDecimal) limitDetails.get("underReview");

            if (pendingDisbursement.compareTo(BigDecimal.ZERO) > 0) {
                errorMsg.append("You have KES ").append(pendingDisbursement)
                       .append(" in loans pending disbursement. ");
            }
            if (underReview.compareTo(BigDecimal.ZERO) > 0) {
                errorMsg.append("You have KES ").append(underReview)
                       .append(" in loans under review. ");
            }

            throw new RuntimeException(errorMsg.toString());
        }

        // Check 3: Has defaults
        if ((boolean) limitDetails.get("hasDefaults")) {
            throw new RuntimeException("Cannot apply for loan while having defaulted or written-off loans. Please clear your defaults first.");
        }
        // Calculate weekly repayment amount
        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
                amount,
                product.getInterestRate(),
                duration,
                Loan.DurationUnit.valueOf(unit)
        );

        Loan loan = Loan.builder()
                .loanNumber("LN" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(amount)
                .duration(duration)
                .durationUnit(Loan.DurationUnit.valueOf(unit))
                .monthlyRepayment(weeklyRepayment) // Store weekly repayment amount
                .status(Loan.LoanStatus.DRAFT)
                .applicationDate(LocalDate.now())
                .votesYes(0).votesNo(0)
                .totalPrepaid(BigDecimal.ZERO).totalArrears(BigDecimal.ZERO)
                .build();

        return convertToDTO(loanRepository.save(loan));
    }
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

        // Recalculate weekly repayment
        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
                amount,
                loan.getProduct().getInterestRate(),
                duration,
                Loan.DurationUnit.valueOf(unit)
        );
        loan.setMonthlyRepayment(weeklyRepayment);

        return convertToDTO(loanRepository.save(loan));
    }

    public LoanDTO applyForLoan(UUID memberId, UUID productId, BigDecimal amount, Integer duration) {
        return initiateApplication(memberId, productId, amount, duration, "MONTHS");
    }
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
    public void submitToGuarantors(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if(loan.getGuarantors() == null || loan.getGuarantors().isEmpty())
            throw new RuntimeException("At least one guarantor required");

        loan.setStatus(Loan.LoanStatus.GUARANTORS_PENDING);
        loanRepository.save(loan);

        // TODO: Notification class not properly imported - commenting out notification creation
        // notificationService.createNotification(
        //         loan.getMember().getUser(),
        //         "Guarantor Requests Sent",
        //         "Your loan application has been sent to the selected guarantors for approval.",
        //         Notification.NotificationType.INFO
        // );

        for (Guarantor g : loan.getGuarantors()) {
            if (g.getMember().getUser() != null) {
                String msg = String.format("Request from %s %s: Please guarantee loan %s for KES %s. Your liability: KES %s",
                        loan.getMember().getFirstName(), loan.getMember().getLastName(),
                        loan.getLoanNumber(), loan.getPrincipalAmount(), g.getGuaranteeAmount());

                // TODO: Notification class not properly imported - commenting out notification creation
                // notificationService.createNotification(
                //         g.getMember().getUser(),
                //         "Guarantorship Request",
                //         msg,
                //         Notification.NotificationType.ACTION_REQUIRED
                // );
            }
        }
    }
    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        System.out.println("🔔 [LoanService] Adding guarantor request...");
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();
        Member applicant = loan.getMember();

        System.out.println("   Applicant: " + applicant.getFirstName() + " " + applicant.getLastName());
        System.out.println("   Guarantor: " + guarantor.getFirstName() + " " + guarantor.getLastName());
        System.out.println("   Amount: KES " + amount);

        // Check 1: Cannot guarantee self
        if(guarantor.getId().equals(loan.getMember().getId()))
            throw new RuntimeException("Cannot guarantee self.");

        // Check 2: Run full eligibility check
        Map<String, Object> eligibility = checkGuarantorEligibility(guarantor, amount);
        if (!(boolean) eligibility.get("eligible")) {
            @SuppressWarnings("unchecked")
            List<String> reasons = (List<String>) eligibility.get("reasons");
            throw new RuntimeException("Guarantor not eligible: " + String.join("; ", reasons));
        }

        // All checks passed - create guarantor request
        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        Guarantor saved = guarantorRepository.save(g);
        System.out.println("✅ [LoanService] Guarantor request saved to database");

        // 🔔 SEND NOTIFICATION TO GUARANTOR
        try {
            String title = "Guarantor Request";
            String message = String.format("%s %s has requested you to guarantee their loan of KES %s with a guarantee amount of KES %s",
                    applicant.getFirstName(),
                    applicant.getLastName(),
                    loan.getPrincipalAmount(),
                    amount);

            System.out.println("📧 [LoanService] Sending notification to guarantor...");
            notificationService.notifyUser(
                    guarantor.getUser().getId(),
                    title,
                    message,
                    true,  // Send email
                    false  // Don't send SMS
            );
            System.out.println("✅ [LoanService] Notification sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ [LoanService] Failed to send notification: " + e.getMessage());
            e.printStackTrace();
            // Don't throw - guarantor request is already saved
        }

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
    public void respondToGuarantorship(UUID guarantorId, boolean accepted) {
        System.out.println("🔔 [LoanService] Processing guarantor response...");
        Guarantor g = guarantorRepository.findById(guarantorId).orElseThrow(() -> new RuntimeException("Request not found"));

        Member applicant = g.getLoan().getMember();
        Member guarantor = g.getMember();

        g.setStatus(accepted ? Guarantor.GuarantorStatus.ACCEPTED : Guarantor.GuarantorStatus.DECLINED);
        g.setDateResponded(LocalDate.now());
        guarantorRepository.save(g);

        System.out.println("✅ [LoanService] Guarantor status updated: " + (accepted ? "ACCEPTED" : "DECLINED"));

        // 🔔 NOTIFY APPLICANT OF GUARANTOR RESPONSE
        try {
            String statusText = accepted ? "accepted" : "declined";
            String title = "Guarantor " + (accepted ? "Approved" : "Declined");
            String message = String.format("%s %s has %s your guarantorship request for KES %s",
                    guarantor.getFirstName(),
                    guarantor.getLastName(),
                    statusText,
                    g.getGuaranteeAmount());

            System.out.println("📧 [LoanService] Notifying applicant of response...");
            notificationService.notifyUser(
                    applicant.getUser().getId(),
                    title,
                    message,
                    true,  // Send email
                    false  // Don't send SMS
            );
            System.out.println("✅ [LoanService] Notification sent to applicant!");
        } catch (Exception e) {
            System.err.println("❌ [LoanService] Failed to send notification: " + e.getMessage());
            e.printStackTrace();
        }

        // Check if all guarantors have responded
        Loan loan = g.getLoan();
        long pending = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.PENDING);
        long declined = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.DECLINED);

        if (pending == 0) {
            if (declined == 0) {
                // All guarantors accepted - move to fee payment stage
                loan.setStatus(Loan.LoanStatus.APPLICATION_FEE_PENDING);
                // Notify member to pay processing fee
                // notificationService.createNotification(loan.getMember().getUser(), "Guarantors Approved", "All guarantors have accepted! Please pay the processing fee to submit your application.", Notification.NotificationType.SUCCESS);
            } else {
                // Some declined - notify member
                // notificationService.createNotification(loan.getMember().getUser(), "Guarantor Update", "All guarantors responded, but some declined. Please review.", Notification.NotificationType.WARNING);
            }
            loanRepository.save(loan);
        }
    }
    public void payApplicationFee(UUID loanId, String refCode) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        // Check if loan is in the correct status for fee payment
        if(loan.getStatus() != Loan.LoanStatus.APPLICATION_FEE_PENDING)
            throw new RuntimeException("Loan must be in APPLICATION_FEE_PENDING status. Current status: " + loan.getStatus());

        BigDecimal fee = loan.getProduct().getProcessingFee();
        if (fee == null) fee = BigDecimal.ZERO;

        // Record transaction
        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .amount(fee)
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA)
                .referenceCode(refCode)
                .description("Loan processing fee - " + loan.getLoanNumber())
                .build();
        transactionRepository.save(tx);

        // ✅ POST TO ACCOUNTING - Creates: DEBIT Cash (1020), CREDIT Fee Income (4030)
        accountingService.postMemberFee(loan.getMember(), fee, "PROCESSING_FEE");

        loan.setApplicationFeePaid(true);
        loan.setStatus(Loan.LoanStatus.SUBMITTED);
        loan.setSubmissionDate(LocalDate.now());
        loanRepository.save(loan);
    }

    // ========================================================================
    // 3. WORKFLOW & APPROVALS (OFFICER -> SECRETARY -> CHAIRPERSON -> ADMIN)
    // ========================================================================
    public LoanDTO officerReview(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        if (loan.getStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new RuntimeException("Loan must be in SUBMITTED status to start review.");
        }
        loan.setStatus(Loan.LoanStatus.LOAN_OFFICER_REVIEW);
        return convertToDTO(loanRepository.save(loan));
    }
    public void officerApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (loan.getStatus() != Loan.LoanStatus.LOAN_OFFICER_REVIEW && loan.getStatus() != Loan.LoanStatus.SUBMITTED) {
            throw new RuntimeException("Loan must be reviewed before approval.");
        }
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loanRepository.save(loan);

        // Notify Secretaries
        // TODO: Notification class not properly imported - commenting out notification creation
        // List<User> secretaries = userRepository.findByRole(User.Role.SECRETARY);
        // for (User secretary : secretaries) {
        //     notificationService.createNotification(
        //             secretary,
        //             "New Loan Tabled",
        //             String.format("Loan %s for %s has been approved by the Loan Officer and is ready for tabling.",
        //                     loan.getLoanNumber(), loan.getMember().getFirstName()),
        //             Notification.NotificationType.ACTION_REQUIRED
        //     );
        // }
    }
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
                System.out.println("âš ï¸ Warning: No users found with role " + role + ". Notification skipped.");
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
    public void openVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ON_AGENDA) {
            throw new RuntimeException("Loan must be tabled on the agenda before voting can start.");
        }

        loan.setVotingOpen(true);
        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        // Ensure list is ready
        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new java.util.ArrayList<>());

        loanRepository.save(loan);
    }

    // 2. CAST VOTE (Strict Rules)
    public void castVote(UUID loanId, boolean voteYes, UUID voterId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.isVotingOpen()) {
            throw new RuntimeException("Voting is closed for this loan.");
        }

        // Get the voter's user record
        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        // Get voter's member record if they have one
        Member voterMember = null;
        if (voter.getMemberNumber() != null) {
            voterMember = memberRepository.findByMemberNumber(voter.getMemberNumber()).orElse(null);
        }

        // ✅ RULE 1: Prevent Double Voting
        if (loan.getVotedUserIds() != null && loan.getVotedUserIds().contains(voterId)) {
            throw new RuntimeException("You have already voted on this loan.");
        }

        // ✅ RULE 2: Conflict of Interest (Self-Voting Blocked)
        // Check if voter's member ID matches loan applicant's member ID
        if (voterMember != null && loan.getMember().getId().equals(voterMember.getId())) {
            throw new RuntimeException("Conflict of Interest: You cannot vote on your own loan application.");
        }

        // Record Vote
        if (voteYes) loan.setVotesYes(loan.getVotesYes() + 1);
        else loan.setVotesNo(loan.getVotesNo() + 1);

        // Add to 'Voted' list
        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new java.util.ArrayList<>());
        loan.getVotedUserIds().add(voterId);

        loanRepository.save(loan);
    }

    // 3. GET AGENDA (Filter out voted loans so card disappears)
    public List<LoanDTO> getVotingAgendaForUser(UUID userId) {
        // Get user's member record
        User user = userRepository.findById(userId).orElse(null);
        Member userMember = null;
        if (user != null && user.getMemberNumber() != null) {
            userMember = memberRepository.findByMemberNumber(user.getMemberNumber()).orElse(null);
        }

        final Member finalUserMember = userMember;

        return loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.VOTING_OPEN) // Must be open
                .filter(l -> l.getVotedUserIds() == null || !l.getVotedUserIds().contains(userId)) // Must NOT have voted
                .filter(l -> finalUserMember == null || !l.getMember().getId().equals(finalUserMember.getId())) // Hide own loan from list
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // CORRECTED VOTING LOGIC
    // ========================================================================
    public void finalizeVote(UUID loanId, Boolean manualApproved, String comments) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.isVotingOpen() && loan.getStatus() != Loan.LoanStatus.VOTING_OPEN) {
            // Allow finalizing if it's already closed but waiting for decision, otherwise warn
            if (loan.getStatus() != Loan.LoanStatus.VOTING_CLOSED) {
                throw new RuntimeException("Voting is not currently active for this loan.");
            }
        }

        loan.setVotingOpen(false);
        loan.setStatus(Loan.LoanStatus.VOTING_CLOSED);
        loanRepository.save(loan); // Save intermediate state

        // 1. PRIORITY: Manual Override (Secretary/Admin Decision)
        // If the official explicitly sends 'true' or 'false', we use that decision regardless of vote counts.
        if (manualApproved != null) {
            if (manualApproved) {
                approveLoanInternal(loan, "Committee Decision: " + comments);
            } else {
                rejectLoanInternal(loan, "Committee Decision: " + comments);
            }
            return;
        }

        // 2. FALLBACK: Automatic Logic (System Settings)
        String votingMethod = systemSettingService.getSetting("LOAN_VOTING_METHOD").orElse("AUTOMATIC");

        if ("AUTOMATIC".equalsIgnoreCase(votingMethod)) {
            // FIX: Use 'Votes Cast' logic instead of 'Total Membership' to prevent getting stuck
            // Logic: Must have more YES than NO, and at least 1 vote cast.

            int totalVotes = loan.getVotesYes() + loan.getVotesNo();
            String resultDetails = String.format("Votes: Yes(%d) vs No(%d).", loan.getVotesYes(), loan.getVotesNo());

            if (totalVotes == 0) {
                // Edge Case: No one voted. Do not Auto-Reject. Move to manual review.
                loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
                loan.setSecretaryComments("Voting closed with 0 votes. Manual decision required.");
                loanRepository.save(loan);
                return;
            }

            if (loan.getVotesYes() > loan.getVotesNo()) {
                approveLoanInternal(loan, "Passed automatic voting (Simple Majority). " + resultDetails);
            } else {
                rejectLoanInternal(loan, "Failed automatic voting. " + resultDetails);
            }
        } else {
            // If Method is MANUAL but no decision was provided in the API call
            loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
            loanRepository.save(loan);
        }
    }

    // Helper to keep code clean and Notify ADMIN
    private void approveLoanInternal(Loan loan, String note) {
        loan.setStatus(Loan.LoanStatus.ADMIN_APPROVED); // Next Step: Admin Approval
        loan.setSecretaryComments(note);
        loanRepository.save(loan);

        // Notify Member
        // TODO: Notification class not properly imported - commenting out notification creation
        // notificationService.createNotification(
        //         loan.getMember().getUser(),
        //         "Loan Voted Successfully",
        //         "Your loan has passed the committee vote! It is now pending final Admin approval.",
        //         Notification.NotificationType.SUCCESS
        // );

        // FIX: Notify ADMIN that they need to give final sign-off
        // List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        // for (User admin : admins) {
        //     notificationService.createNotification(
        //             admin,
        //             "Final Approval Required",
        //             String.format("Loan %s has passed voting and requires your final sign-off.", loan.getLoanNumber()),
        //             Notification.NotificationType.ACTION_REQUIRED
        //     );
        // }
    }

    private void rejectLoanInternal(Loan loan, String reason) {
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        loanRepository.save(loan);

        // notificationService.createNotification(
        //         loan.getMember().getUser(),
        //         "Loan Rejected",
        //         reason,
        //         Notification.NotificationType.ERROR
        // );
    }

    // ========================================================================
    // CORRECTED ADMIN APPROVAL
    // ========================================================================
    public void adminApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        // FIX: Ensure we don't skip the voting stage accidentally
        // We allow ADMIN_APPROVED (Standard flow) or SECRETARY_DECISION (Fallback flow)
        if (loan.getStatus() != Loan.LoanStatus.ADMIN_APPROVED &&
                loan.getStatus() != Loan.LoanStatus.SECRETARY_DECISION) {
            throw new RuntimeException("Loan is not in the correct state for Final Approval. Current: " + loan.getStatus());
        }

        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loan.setApprovalDate(LocalDate.now());
        loanRepository.save(loan);

        // FIX: Notify Treasurer
        // TODO: Notification class not properly imported - commenting out notification creation
        // List<User> treasurers = userRepository.findByRole(User.Role.TREASURER);
        // for (User treasurer : treasurers) {
        //     notificationService.createNotification(
        //             treasurer,
        //             "Disbursement Pending",
        //             String.format("Loan %s is fully approved and ready for disbursement.", loan.getLoanNumber()),
        //             Notification.NotificationType.ACTION_REQUIRED
        //     );
        // }
    }
    public void treasurerDisburse(UUID loanId, String checkNumber) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        BigDecimal currentLiquidity = accountingService.getAccountBalance("1001", LocalDate.now());
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
    public LoanDTO repayLoan(UUID loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        repaymentService.processPayment(loan, amount);
        return convertToDTO(loanRepository.save(loan));
    }

    // ========================================================================
    // 5. HELPERS
    // ========================================================================

    public LoanDTO approveLoan(UUID id) { officerApprove(id); return getLoanById(id); }

    //  FIX: Updated to use 'finalizeVote' instead of 'secretaryFinalize'
    public LoanDTO rejectLoan(UUID id) { finalizeVote(id, false, "Rejected"); return getLoanById(id); }

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

    // CORRECTED DTO CONVERSION (MOVED TO BOTTOM, DUPLICATE REMOVED)
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
                // ✅ MAP VOTES
                .votesYes(loan.getVotesYes())
                .votesNo(loan.getVotesNo())
                .build();
    }

    /**
     * Get loan repository (for calculator/automation services)
     */
    public LoanRepository getLoanRepository() {
        return loanRepository;
    }

    /**
     * Get loan entity by ID
     */
    public Loan getLoanEntity(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + id));
    }
}




