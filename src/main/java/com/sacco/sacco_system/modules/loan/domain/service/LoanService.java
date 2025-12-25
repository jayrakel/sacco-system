package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.notification.domain.service.NotificationService;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
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
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
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

    public Map<String, Object> checkLoanEligibility(Member member) {
        Map<String, Object> result = new HashMap<>();
        List<String> failureReasons = new ArrayList<>();
        boolean eligible = true;

        BigDecimal minSavings = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SAVINGS_FOR_LOAN"));
        int minMonths = (int) systemSettingService.getDouble("MIN_MONTHS_MEMBERSHIP");
        BigDecimal minShareCapital = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SHARE_CAPITAL"));

        BigDecimal currentSavings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
        if (currentSavings.compareTo(minSavings) < 0) {
            eligible = false;
            failureReasons.add("Insufficient savings. Required: KES " + minSavings.toPlainString() +
                ", Current: KES " + currentSavings.toPlainString());
        }

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

        BigDecimal currentShareCapital = member.getTotalShares() != null ? member.getTotalShares() : BigDecimal.ZERO;
        if (minShareCapital.compareTo(BigDecimal.ZERO) > 0 && currentShareCapital.compareTo(minShareCapital) < 0) {
            eligible = false;
            failureReasons.add("Insufficient share capital. Required: KES " + minShareCapital.toPlainString() +
                ", Current: KES " + currentShareCapital.toPlainString());
        }

        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            eligible = false;
            failureReasons.add("Member account is not active. Current status: " + member.getStatus());
        }

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

    @Deprecated
    public Map<String, Object> checkApplicationFeeStatus(Member member) {
        Map<String, Object> result = new HashMap<>();
        result.put("feePaid", false);
        result.put("hasDraft", false);
        result.put("message", "Fee payment is now done after guarantor approval");
        return result;
    }

    @Deprecated
    @Transactional
    public LoanDTO payApplicationFeeAndCreateDraft(Member member, String referenceCode) {
        throw new RuntimeException("This method is deprecated. Fee payment is now done after guarantor approval.");
    }

    public Map<String, Object> checkGuarantorEligibility(Member member, BigDecimal guaranteeAmount) {
        Map<String, Object> result = new HashMap<>();
        List<String> failureReasons = new ArrayList<>();
        boolean eligible = true;

        BigDecimal minSavings = BigDecimal.valueOf(systemSettingService.getDouble("MIN_SAVINGS_TO_GUARANTEE"));
        int minMonths = (int) systemSettingService.getDouble("MIN_MONTHS_TO_GUARANTEE");
        double maxGuarantorRatio = systemSettingService.getDouble("MAX_GUARANTOR_LIMIT_RATIO");

        BigDecimal currentSavings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
        if (currentSavings.compareTo(minSavings) < 0) {
            eligible = false;
            failureReasons.add("Insufficient savings to guarantee. Required: KES " + minSavings.toPlainString() +
                ", Current: KES " + currentSavings.toPlainString());
        }

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

        if (guaranteeAmount != null && currentSavings.compareTo(guaranteeAmount) < 0) {
            eligible = false;
            failureReasons.add("Cannot guarantee KES " + guaranteeAmount.toPlainString() +
                " with only KES " + currentSavings.toPlainString() + " in savings");
        }

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

        if (member.getStatus() != Member.MemberStatus.ACTIVE) {
            eligible = false;
            failureReasons.add("Member account is not active. Current status: " + member.getStatus());
        }

        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        boolean hasDefault = memberLoans.stream()
            .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED);

        if (hasDefault) {
            eligible = false;
            failureReasons.add("Cannot guarantee while having defaulted loans");
        }

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

    // ========================================================================
    // ✅ UPDATED: APPLY FOR LOAN (WITH 1/3RD RULE)
    // ========================================================================
    public LoanDTO initiateApplication(UUID memberId, UUID productId, BigDecimal amount, Integer duration, String unit) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        LoanProduct product = loanProductRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        // 1. Check Product Limit
        if (amount.compareTo(product.getMaxLimit()) > 0)
            throw new RuntimeException("Amount exceeds product limit of " + product.getMaxLimit());

        // 2. Check General Limit (Savings Multiplier + Salary Limit + Existing Debt)
        Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(member);
        BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");

        if (amount.compareTo(availableLimit) > 0) {
            String error = "Amount exceeds your available limit of KES " + availableLimit;
            BigDecimal salaryLimit = (BigDecimal) limitDetails.get("salaryBasedLimit");
            BigDecimal savingsLimit = (BigDecimal) limitDetails.get("savingsBasedLimit");
            
            if (salaryLimit != null && salaryLimit.compareTo(savingsLimit) < 0) {
                error += ". Limit is restricted by your Net Salary (1/3rd Rule).";
            } else {
                error += ". Limit is restricted by your Total Savings (Multiplier).";
            }
            throw new RuntimeException(error);
        }

        if ((boolean) limitDetails.get("hasDefaults")) {
            throw new RuntimeException("Cannot apply for loan while having defaulted or written-off loans. Please clear your defaults first.");
        }
        
        // 3. Calculate Repayment
        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
                amount,
                product.getInterestRate(),
                duration,
                Loan.DurationUnit.valueOf(unit)
        );
        
        // 4. ✅ CHECK: Ability to Pay (1/3rd Rule)
        BigDecimal monthlyRepayment = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
            BigDecimal netIncome = emp.getNetMonthlyIncome();
            double maxDebtRatio = systemSettingService.getDouble("MAX_DEBT_RATIO", 0.66);
            BigDecimal maxAllowableRepayment = netIncome.multiply(BigDecimal.valueOf(maxDebtRatio));

            if (monthlyRepayment.compareTo(maxAllowableRepayment) > 0) {
                throw new RuntimeException(String.format(
                    "Loan declined by 1/3rd Rule. Estimated monthly repayment (KES %.2f) exceeds 2/3rds of your net income (KES %.2f). Please extend the duration or reduce the amount.", 
                    monthlyRepayment, maxAllowableRepayment
                ));
            }
        } 

        // 5. Create Application
        Loan loan = Loan.builder()
                .loanNumber("LN" + System.currentTimeMillis())
                .member(member)
                .product(product)
                .principalAmount(amount)
                .duration(duration)
                .durationUnit(Loan.DurationUnit.valueOf(unit))
                .weeklyRepaymentAmount(weeklyRepayment)
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

        BigDecimal weeklyRepayment = repaymentScheduleService.calculateWeeklyRepayment(
                amount,
                loan.getProduct().getInterestRate(),
                duration,
                Loan.DurationUnit.valueOf(unit)
        );

        EmploymentDetails emp = loan.getMember().getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null) {
             BigDecimal monthly = weeklyRepayment.multiply(BigDecimal.valueOf(4.33));
             BigDecimal max = emp.getNetMonthlyIncome().multiply(BigDecimal.valueOf(0.66));
             if (monthly.compareTo(max) > 0) {
                 throw new RuntimeException("New repayment exceeds income ability (1/3rd rule).");
             }
        }

        loan.setPrincipalAmount(amount);
        loan.setDuration(duration);
        loan.setDurationUnit(Loan.DurationUnit.valueOf(unit));
        loan.setWeeklyRepaymentAmount(weeklyRepayment);
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

        userRepository.findByEmail(loan.getMember().getEmail()).ifPresent(user -> {
            notificationService.notifyUser(
                    user.getId(),
                    "Guarantor Requests Sent",
                    "Your loan application has been sent to the selected guarantors for approval.",
                    true, false
            );
        });

        for (Guarantor g : loan.getGuarantors()) {
            if (g.getMember().getEmail() != null) {
                String msg = String.format("Request from %s %s: Please guarantee loan %s for KES %s. Your liability: KES %s",
                        loan.getMember().getFirstName(), loan.getMember().getLastName(),
                        loan.getLoanNumber(), loan.getPrincipalAmount(), g.getGuaranteeAmount());

                userRepository.findByEmail(g.getMember().getEmail()).ifPresent(user -> {
                    notificationService.notifyUser(
                            user.getId(),
                            "Guarantorship Request",
                            msg,
                            true,   // Send email
                            false   // Don't send SMS
                    );
                });
            }
        }
    }

    public GuarantorDTO addGuarantor(UUID loanId, UUID guarantorMemberId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        Member guarantor = memberRepository.findById(guarantorMemberId).orElseThrow();
        Member applicant = loan.getMember();

        if(guarantor.getId().equals(loan.getMember().getId()))
            throw new RuntimeException("Cannot guarantee self.");

        Map<String, Object> eligibility = checkGuarantorEligibility(guarantor, amount);
        if (!(boolean) eligibility.get("eligible")) {
            @SuppressWarnings("unchecked")
            List<String> reasons = (List<String>) eligibility.get("reasons");
            throw new RuntimeException("Guarantor not eligible: " + String.join("; ", reasons));
        }

        Guarantor g = Guarantor.builder()
                .loan(loan)
                .member(guarantor)
                .guaranteeAmount(amount)
                .status(Guarantor.GuarantorStatus.PENDING)
                .dateRequestSent(LocalDate.now())
                .build();

        Guarantor saved = guarantorRepository.save(g);
        
        try {
            String title = "Guarantor Request";
            String message = String.format("%s %s has requested you to guarantee their loan of KES %s with a guarantee amount of KES %s",
                    applicant.getFirstName(),
                    applicant.getLastName(),
                    loan.getPrincipalAmount(),
                    amount);

            userRepository.findByEmail(guarantor.getEmail()).ifPresent(user -> {
                notificationService.notifyUser(
                        user.getId(),
                        title,
                        message,
                        true,  // Send email
                        false  // Don't send SMS
                );
            });
        } catch (Exception e) {
            System.err.println("❌ [LoanService] Failed to send notification: " + e.getMessage());
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
        Guarantor g = guarantorRepository.findById(guarantorId).orElseThrow(() -> new RuntimeException("Request not found"));

        Member applicant = g.getLoan().getMember();
        Member guarantor = g.getMember();

        g.setStatus(accepted ? Guarantor.GuarantorStatus.ACCEPTED : Guarantor.GuarantorStatus.DECLINED);
        g.setDateResponded(LocalDate.now());
        guarantorRepository.save(g);

        try {
            String statusText = accepted ? "accepted" : "declined";
            String title = "Guarantor " + (accepted ? "Approved" : "Declined");
            String message = String.format("%s %s has %s your guarantorship request for KES %s",
                    guarantor.getFirstName(),
                    guarantor.getLastName(),
                    statusText,
                    g.getGuaranteeAmount());

            userRepository.findByEmail(applicant.getEmail()).ifPresent(user -> {
                notificationService.notifyUser(
                        user.getId(),
                        title,
                        message,
                        true,  // Send email
                        false  // Don't send SMS
                );
            });
        } catch (Exception e) {
            System.err.println("❌ [LoanService] Failed to send notification: " + e.getMessage());
        }

        Loan loan = g.getLoan();
        long pending = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.PENDING);
        long declined = guarantorRepository.countByLoanAndStatus(loan, Guarantor.GuarantorStatus.DECLINED);

        if (pending == 0) {
            if (declined == 0) {
                loan.setStatus(Loan.LoanStatus.APPLICATION_FEE_PENDING);
                
                userRepository.findByEmail(loan.getMember().getEmail()).ifPresent(user -> {
                    notificationService.notifyUser(
                        user.getId(), 
                        "Guarantors Approved", 
                        "All guarantors have accepted! Please pay the processing fee to submit your application.", 
                        true, false
                    );
                });
            } else {
                userRepository.findByEmail(loan.getMember().getEmail()).ifPresent(user -> {
                    notificationService.notifyUser(
                        user.getId(), 
                        "Guarantor Update", 
                        "All guarantors responded, but some declined. Please review your application.", 
                        true, false
                    );
                });
            }
            loanRepository.save(loan);
        }
    }

    public void payApplicationFee(UUID loanId, String paymentMethod, String refCode) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();

        if(loan.getStatus() != Loan.LoanStatus.APPLICATION_FEE_PENDING)
            throw new RuntimeException("Loan must be in APPLICATION_FEE_PENDING status. Current: " + loan.getStatus());

        BigDecimal fee = loan.getProduct().getProcessingFee();
        if (fee == null) fee = BigDecimal.ZERO;

        Transaction.PaymentMethod method = Transaction.PaymentMethod.CASH;
        if(paymentMethod != null) {
            try {
                method = Transaction.PaymentMethod.valueOf(paymentMethod.toUpperCase());
            } catch (Exception e) {}
        }

        String sourceGlAccount = getDebitAccountForPayment(method.toString());

        Transaction tx = Transaction.builder()
                .member(loan.getMember())
                .amount(fee)
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(method)
                .referenceCode(refCode)
                .description("Loan processing fee - " + loan.getLoanNumber())
                .build();
        transactionRepository.save(tx);

        accountingService.postEvent(
            "PROCESSING_FEE", 
            "Loan processing fee - " + loan.getLoanNumber(), 
            refCode, 
            fee, 
            sourceGlAccount
        );

        loan.setApplicationFeePaid(true);
        loan.setStatus(Loan.LoanStatus.SUBMITTED);
        loan.setSubmissionDate(LocalDate.now());
        loanRepository.save(loan);
    }

    private String getDebitAccountForPayment(String paymentMethod) {
        if (paymentMethod == null) return "1001"; // Default Cash
        
        switch (paymentMethod.toUpperCase()) {
            case "MPESA": return "1002"; // M-Pesa Control Account
            case "BANK": return "1010";  // Default Bank (e.g. Equity)
            case "CASH": return "1001";  // Teller Cash
            default: return "1001";
        }
    }

    // ========================================================================
    // 3. WORKFLOW & APPROVALS (OFFICER -> SECRETARY -> CHAIRPERSON -> SECRETARY -> TREASURER)
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
        
        // 1. Officer Approves -> Sends to Secretary for Tabling
        loan.setStatus(Loan.LoanStatus.SECRETARY_TABLED);
        loanRepository.save(loan);

        // Notify Secretaries
        List<User> secretaries = userRepository.findByRole(User.Role.SECRETARY);
        for (User secretary : secretaries) {
            notificationService.notifyUser(
                    secretary.getId(),
                    "New Loan Tabled",
                    String.format("Loan %s for %s has been approved by the Loan Officer and is ready for tabling.",
                            loan.getLoanNumber(), loan.getMember().getFirstName()),
                    true, false
            );
        }
    }

    public void tableLoan(UUID loanId, LocalDate meetingDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_TABLED) {
            throw new RuntimeException("Loan is not ready for tabling.");
        }

        // 2. Secretary Tables Loan -> Sends to Chairperson (ON_AGENDA)
        loan.setMeetingDate(meetingDate);
        loan.setStatus(Loan.LoanStatus.ON_AGENDA);
        loanRepository.save(loan);

        // Notify Chairperson
        List<User> chairpeople = userRepository.findByRole(User.Role.CHAIRPERSON);
        for (User chair : chairpeople) {
            notificationService.notifyUser(
                    chair.getId(),
                    "New Agenda Item",
                    String.format("Loan Ref %s has been tabled for the meeting on %s.",
                            loan.getLoanNumber(), meetingDate),
                    true, false
            );
        }
        
        // Notify Member
        userRepository.findByEmail(loan.getMember().getEmail()).ifPresent(user -> {
            notificationService.notifyUser(
                    user.getId(),
                    "Application Tabled",
                    "Your loan application has been added to the agenda for the committee meeting on " + meetingDate,
                    true, false
            );
        });
    }

    public void openVoting(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ON_AGENDA) {
            throw new RuntimeException("Loan must be tabled on the agenda before voting can start.");
        }

        // ✅ CHECK: Prevent opening voting before the meeting date
        if (loan.getMeetingDate() != null && LocalDate.now().isBefore(loan.getMeetingDate())) {
            throw new RuntimeException("Cannot open voting yet. Meeting date is " + loan.getMeetingDate());
        }

        // 3. Chairperson Opens Voting (When Meeting is Due)
        loan.setVotingOpen(true);
        loan.setStatus(Loan.LoanStatus.VOTING_OPEN);
        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new java.util.ArrayList<>());

        loanRepository.save(loan);
    }

    public void castVote(UUID loanId, boolean voteYes, UUID voterId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (!loan.isVotingOpen()) {
            throw new RuntimeException("Voting is closed for this loan.");
        }

        User voter = userRepository.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));

        Member voterMember = memberRepository.findByEmail(voter.getEmail()).orElse(null);

        if (voterMember == null) {
            throw new RuntimeException("No member profile found for this account. Please log in to your Member account to vote.");
        }

        if (loan.getVotedUserIds() != null && loan.getVotedUserIds().contains(voterId)) {
            throw new RuntimeException("You have already voted on this loan.");
        }

        if (loan.getMember().getId().equals(voterMember.getId())) {
            throw new RuntimeException("Conflict of Interest: You cannot vote on your own loan application.");
        }

        if (voteYes) loan.setVotesYes(loan.getVotesYes() + 1);
        else loan.setVotesNo(loan.getVotesNo() + 1);

        if (loan.getVotedUserIds() == null) loan.setVotedUserIds(new java.util.ArrayList<>());
        loan.getVotedUserIds().add(voterId);

        loanRepository.save(loan);
    }

    public List<LoanDTO> getVotingAgendaForUser(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        Member userMember = null;
        if (user != null && user.getEmail() != null) {
            userMember = memberRepository.findByEmail(user.getEmail()).orElse(null);
        }

        final Member finalUserMember = userMember;

        return loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.VOTING_OPEN) 
                .filter(l -> l.getVotedUserIds() == null || !l.getVotedUserIds().contains(userId)) 
                .filter(l -> finalUserMember == null || !l.getMember().getId().equals(finalUserMember.getId())) 
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void finalizeVote(UUID loanId, Boolean manualApproved, String comments) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        loan.setVotingOpen(false);
        loan.setStatus(Loan.LoanStatus.VOTING_CLOSED);
        loanRepository.save(loan); 

        if (manualApproved != null) {
            if (manualApproved) {
                approveLoanInternal(loan, "Committee Manual Decision: " + comments);
            } else {
                rejectLoanInternal(loan, "Committee Manual Decision: " + comments);
            }
            return;
        }

        String votingMethod = systemSettingService.getSetting("LOAN_VOTING_METHOD").orElse("AUTOMATIC");

        if ("AUTOMATIC".equalsIgnoreCase(votingMethod)) {
            int totalVotes = loan.getVotesYes() + loan.getVotesNo();
            String resultDetails = String.format("Votes: Yes(%d) vs No(%d).", loan.getVotesYes(), loan.getVotesNo());

            if (totalVotes == 0) {
                loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
                loan.setSecretaryComments("Voting closed with 0 votes. Secretary decision required.");
                loanRepository.save(loan);
                return;
            }

            if (loan.getVotesYes() > loan.getVotesNo()) {
                // ✅ VOTE PASSED -> Goes to Secretary for Final Ratification
                approveLoanInternal(loan, "Passed automatic voting (Simple Majority). " + resultDetails);
            } else {
                rejectLoanInternal(loan, "Failed automatic voting. " + resultDetails);
            }
        } else {
            loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION);
            loanRepository.save(loan);
        }
    }

    // ✅ UPDATED: Sends to Secretary instead of Admin
    private void approveLoanInternal(Loan loan, String note) {
        loan.setStatus(Loan.LoanStatus.SECRETARY_DECISION); // Was ADMIN_APPROVED
        loan.setSecretaryComments(note);
        loanRepository.save(loan);

        List<User> secretaries = userRepository.findByRole(User.Role.SECRETARY);
        for (User secretary : secretaries) {
            notificationService.notifyUser(
                    secretary.getId(),
                    "Voting Passed - Action Required",
                    String.format("Loan %s has passed voting. Please perform final approval for disbursement.", loan.getLoanNumber()),
                    true, false
            );
        }
    }

    private void rejectLoanInternal(Loan loan, String reason) {
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        loanRepository.save(loan);
        
        userRepository.findByEmail(loan.getMember().getEmail()).ifPresent(user -> {
            notificationService.notifyUser(
                    user.getId(),
                    "Loan Rejected",
                    "We regret to inform you that your loan application was rejected. Reason: " + reason,
                    true, false
            );
        });
    }

    // ========================================================================
    // ✅ RENAMED: Secretary Final Approval (Was adminApprove)
    // ========================================================================
    public void secretaryFinalApprove(UUID loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.SECRETARY_DECISION) {
            throw new RuntimeException("Loan is not awaiting final approval. Current status: " + loan.getStatus());
        }

        // 4. Secretary Approves -> Sends to Treasurer
        loan.setStatus(Loan.LoanStatus.TREASURER_DISBURSEMENT);
        loan.setApprovalDate(LocalDate.now());
        loanRepository.save(loan);

        // Notify Treasurer
        List<User> treasurers = userRepository.findByRole(User.Role.TREASURER);
        for (User treasurer : treasurers) {
            notificationService.notifyUser(
                    treasurer.getId(),
                    "Disbursement Pending",
                    String.format("Loan %s is fully approved and ready for disbursement.", loan.getLoanNumber()),
                    true, false
            );
        }
    }

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
    
    public LoanDTO repayLoan(UUID loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        repaymentService.processPayment(loan, amount);
        return convertToDTO(loanRepository.save(loan));
    }

    // ========================================================================
    // 5. HELPERS
    // ========================================================================

    public LoanDTO approveLoan(UUID id) { officerApprove(id); return getLoanById(id); }

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
                .votesYes(loan.getVotesYes())
                .votesNo(loan.getVotesNo())
                .build();
    }

    public LoanRepository getLoanRepository() {
        return loanRepository;
    }

    public Loan getLoanEntity(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + id));
    }
}