package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanLimitService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanCalculatorService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanAutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanLimitService loanLimitService;
    private final LoanCalculatorService loanCalculatorService;
    private final LoanAutomationService loanAutomationService;
    private final LoanProductRepository loanProductRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService systemSettingService;

    // ========================================================================
    // 1. LOAN PRODUCTS
    // ========================================================================

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanProductRepository.findAll()));
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody LoanProduct product) {
        try {
            LoanProduct saved = loanProductRepository.save(product);
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            auditService.logSuccess(user, AuditLog.Actions.CREATE, "LoanProduct", saved.getId().toString(), "Created: " + saved.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Product Created", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 2. LOAN APPLICATION WORKFLOW
    // ========================================================================

    @GetMapping("/limits/check")
    public ResponseEntity<Map<String, Object>> checkLoanLimit() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        BigDecimal limit = loanLimitService.calculateMemberLoanLimit(member);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "limit", limit,
                "savings", member.getTotalSavings()
        ));
    }

    @GetMapping("/eligibility/check")
    public ResponseEntity<Map<String, Object>> checkLoanEligibility() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> eligibilityResult = loanService.checkLoanEligibility(member);
            return ResponseEntity.ok(eligibilityResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/check-fee-status")
    public ResponseEntity<Map<String, Object>> checkFeeStatus() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> feeStatus = loanService.checkApplicationFeeStatus(member);
            return ResponseEntity.ok(feeStatus);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/application-fee")
    public ResponseEntity<Map<String, Object>> getApplicationFee() {
        try {
            BigDecimal fee = BigDecimal.valueOf(systemSettingService.getDouble("LOAN_APPLICATION_FEE"));
            return ResponseEntity.ok(Map.of(
                "success", true,
                "amount", fee,
                "message", "Loan application fee amount"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/pay-application-fee")
    public ResponseEntity<Map<String, Object>> payApplicationFee(@RequestParam String referenceCode) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            LoanDTO draftLoan = loanService.payApplicationFeeAndCreateDraft(member, referenceCode);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fee paid successfully. Draft application created.",
                "data", draftLoan
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/guarantor/check-eligibility")
    public ResponseEntity<Map<String, Object>> checkGuarantorEligibility(
            @RequestParam UUID memberId,
            @RequestParam BigDecimal guaranteeAmount) {
        try {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> eligibilityResult = loanService.checkGuarantorEligibility(member, guaranteeAmount);
            return ResponseEntity.ok(eligibilityResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyForLoan(
            @RequestParam UUID productId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer duration,
            @RequestParam(defaultValue = "MONTHS") String durationUnit) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            LoanDTO loan = loanService.initiateApplication(member.getId(), productId, amount, duration, durationUnit);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Loan Draft Created", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<Map<String, Object>> updateLoanDraft(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount,
            @RequestParam Integer duration,
            @RequestParam String durationUnit) {
        try {
            LoanDTO loan = loanService.updateApplication(id, amount, duration, durationUnit);
            return ResponseEntity.ok(Map.of("success", true, "message", "Draft Updated", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<Map<String, Object>> addGuarantor(
            @PathVariable UUID loanId,
            @RequestBody Map<String, Object> payload) {
        try {
            UUID memberId = UUID.fromString(payload.get("memberId").toString());
            BigDecimal amount = new BigDecimal(payload.get("guaranteeAmount").toString());

            GuarantorDTO responseDTO = loanService.addGuarantor(loanId, memberId, amount);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Guarantor added", "data", responseDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-requests")
    public ResponseEntity<Map<String, Object>> sendGuarantorRequests(@PathVariable UUID id) {
        try {
            loanService.submitToGuarantors(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Requests sent to guarantors"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/guarantors/requests")
    public ResponseEntity<Map<String, Object>> getMyGuarantorRequests() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            var memberOpt = memberRepository.findByEmail(email);

            if (memberOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "data", List.of()));
            }

            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getPendingGuarantorRequests(memberOpt.get().getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/guarantors/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToGuarantorRequest(@PathVariable UUID id, @RequestParam boolean accepted) {
        try {
            loanService.respondToGuarantorship(id, accepted);
            return ResponseEntity.ok(Map.of("success", true, "message", accepted ? "Request Accepted" : "Request Declined"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay-fee")
    public ResponseEntity<Map<String, Object>> payLoanFee(
            @PathVariable UUID id,
            @RequestParam String referenceCode) {
        try {
            loanService.payApplicationFee(id, referenceCode);
            return ResponseEntity.ok(Map.of("success", true, "message", "Fee Paid & Application Submitted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 3. APPROVAL & DISBURSEMENT
    // ========================================================================

    @PostMapping("/{id}/review")
    public ResponseEntity<Map<String, Object>> reviewLoan(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review Started",
                    "data", loanService.officerReview(id)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // LOAN OFFICER: DETAILED REVIEW
    // ========================================================================

    /**
     * Get comprehensive loan review details for loan officer
     * Shows all factors that should be considered before approval
     */
    @GetMapping("/{id}/review-details")
    public ResponseEntity<Map<String, Object>> getLoanReviewDetails(@PathVariable UUID id) {
        try {
            // Get loan details
            LoanDTO loan = loanService.getLoanById(id);
            Member member = memberRepository.findById(UUID.fromString(loan.getMemberId().toString()))
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            Map<String, Object> review = new HashMap<>();

            // 1. Basic Loan Information
            review.put("loan", loan);

            // 2. Member Information
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("memberNumber", member.getMemberNumber());
            memberInfo.put("fullName", member.getFirstName() + " " + member.getLastName());
            memberInfo.put("status", member.getStatus());
            memberInfo.put("totalSavings", member.getTotalSavings());
            memberInfo.put("totalShares", member.getTotalShares());
            memberInfo.put("memberSince", member.getCreatedAt());
            review.put("memberInfo", memberInfo);

            // 3. STRICT Loan Limit Calculation with Breakdown
            Map<String, Object> limitDetails = loanLimitService.calculateMemberLoanLimitWithDetails(member);
            review.put("loanLimitAnalysis", limitDetails);

            // 4. Guarantor Analysis
            List<GuarantorDTO> guarantors = loanService.getLoanGuarantors(id);
            review.put("guarantors", guarantors);
            review.put("guarantorCount", guarantors.size());

            long acceptedCount = guarantors.stream()
                    .filter(g -> "ACCEPTED".equals(g.getStatus()))
                    .count();
            long pendingCount = guarantors.stream()
                    .filter(g -> "PENDING".equals(g.getStatus()))
                    .count();
            long declinedCount = guarantors.stream()
                    .filter(g -> "DECLINED".equals(g.getStatus()))
                    .count();

            review.put("guarantorsAccepted", acceptedCount);
            review.put("guarantorsPending", pendingCount);
            review.put("guarantorsDeclined", declinedCount);

            // 5. All Member Loans (History Check)
            List<LoanDTO> memberLoans = loanService.getLoansByMemberId(member.getId());
            review.put("totalLoansApplied", memberLoans.size());

            // Break down by status
            long activeLoans = memberLoans.stream()
                    .filter(l -> "ACTIVE".equals(l.getStatus()) || "DISBURSED".equals(l.getStatus()))
                    .count();
            long completedLoans = memberLoans.stream()
                    .filter(l -> "COMPLETED".equals(l.getStatus()))
                    .count();
            long defaultedLoans = memberLoans.stream()
                    .filter(l -> "DEFAULTED".equals(l.getStatus()) || "WRITTEN_OFF".equals(l.getStatus()))
                    .count();
            long pendingApprovalLoans = memberLoans.stream()
                    .filter(l -> l.getStatus().contains("PENDING") ||
                                l.getStatus().contains("REVIEW") ||
                                l.getStatus().contains("TABLED") ||
                                l.getStatus().contains("VOTING") ||
                                l.getStatus().contains("APPROVED") ||
                                l.getStatus().contains("DISBURSEMENT"))
                    .count();

            review.put("activeLoans", activeLoans);
            review.put("completedLoans", completedLoans);
            review.put("defaultedLoans", defaultedLoans);
            review.put("pendingApprovalLoans", pendingApprovalLoans - 1); // Exclude current loan

            // 6. Risk Assessment Flags
            List<String> riskFlags = new java.util.ArrayList<>();
            List<String> approvalChecks = new java.util.ArrayList<>();

            // Check 1: Has defaults
            if ((boolean) limitDetails.get("hasDefaults")) {
                riskFlags.add("⛔ Member has defaulted loans - HIGH RISK");
            } else {
                approvalChecks.add("✅ No loan defaults");
            }

            // Check 2: Exceeds limit
            BigDecimal availableLimit = (BigDecimal) limitDetails.get("availableLimit");
            BigDecimal requestedAmount = loan.getPrincipalAmount();
            if (requestedAmount.compareTo(availableLimit) > 0) {
                riskFlags.add("⚠️ Requested amount (KES " + requestedAmount + ") exceeds available limit (KES " + availableLimit + ")");
            } else {
                approvalChecks.add("✅ Amount within available limit");
            }

            // Check 3: Has other pending loans
            if (pendingApprovalLoans > 1) {
                riskFlags.add("⚠️ Member has " + (pendingApprovalLoans - 1) + " other loan(s) pending approval/disbursement");
            } else {
                approvalChecks.add("✅ No other pending loan applications");
            }

            // Check 4: Application fee paid
            if (loan.getProcessingFee() != null && loan.getProcessingFee().compareTo(BigDecimal.ZERO) > 0) {
                approvalChecks.add("✅ Application fee paid");
            }

            // Check 5: All guarantors accepted
            if (declinedCount > 0) {
                riskFlags.add("⚠️ " + declinedCount + " guarantor(s) declined");
            } else if (pendingCount > 0) {
                riskFlags.add("⏳ " + pendingCount + " guarantor(s) still pending");
            } else if (acceptedCount == guarantors.size() && acceptedCount > 0) {
                approvalChecks.add("✅ All guarantors accepted");
            }

            // Check 6: Member account status
            if (member.getStatus() == Member.MemberStatus.ACTIVE) {
                approvalChecks.add("✅ Member account is active");
            } else {
                riskFlags.add("⛔ Member account is " + member.getStatus());
            }

            review.put("riskFlags", riskFlags);
            review.put("approvalChecks", approvalChecks);

            // 7. Final Recommendation
            boolean recommended = riskFlags.isEmpty() &&
                                 availableLimit.compareTo(requestedAmount) >= 0 &&
                                 !(boolean) limitDetails.get("hasDefaults");

            review.put("recommendedForApproval", recommended);
            review.put("recommendation", recommended ?
                "✅ This loan meets all criteria and can be approved" :
                "⚠️ Please review risk flags before approving");

            return ResponseEntity.ok(Map.of("success", true, "data", review));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable UUID id) {
        try {
            loanService.officerApprove(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Forwarded to Secretary"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<Map<String, Object>> disburseLoan(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Disbursed", "data", loanService.disburseLoan(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectLoan(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Rejected", "data", loanService.rejectLoan(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/write-off")
    public ResponseEntity<Map<String, Object>> writeOffLoan(@PathVariable UUID id, @RequestParam String reason) {
        try {
            loanService.writeOffLoan(id, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Written Off"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/restructure")
    public ResponseEntity<Map<String, Object>> restructureLoan(@PathVariable UUID id, @RequestParam Integer newDuration) {
        try {
            LoanDTO loan = loanService.restructureLoan(id, newDuration);
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            auditService.logSuccess(user, "RESTRUCTURE_LOAN", "Loan", id.toString(), "New Duration: " + newDuration);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Restructured", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<Map<String, Object>> repayLoan(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        try {
            LoanDTO loan = loanService.repayLoan(id, amount);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Repayment processed successfully");
            response.put("remainingLoanBalance", loan.getLoanBalance());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 4. QUERIES
    // ========================================================================

    @GetMapping("/my-loans")
    public ResponseEntity<Map<String, Object>> getMyLoans() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoansByMemberId(member.getId())));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLoans() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getAllLoans()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLoanById(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoanById(id)));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberLoans(@PathVariable UUID memberId) {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoansByMemberId(memberId)));
    }

    @GetMapping("/totals/disbursed")
    public ResponseEntity<Map<String, Object>> getTotalDisbursed() {
        return ResponseEntity.ok(Map.of("success", true, "totalDisbursed", loanService.getTotalDisbursedLoans()));
    }

    @GetMapping("/totals/outstanding")
    public ResponseEntity<Map<String, Object>> getTotalOutstanding() {
        return ResponseEntity.ok(Map.of("success", true, "totalOutstanding", loanService.getTotalOutstandingLoans()));
    }

    @GetMapping("/totals/interest")
    public ResponseEntity<Map<String, Object>> getTotalInterest() {
        return ResponseEntity.ok(Map.of("success", true, "totalInterest", loanService.getTotalInterestCollected()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteLoan(@PathVariable UUID id) {
        try {
            loanService.deleteApplication(id);
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            auditService.logSuccess(user, AuditLog.Actions.DELETE, "Loan", id.toString(), "Application Deleted");
            return ResponseEntity.ok(Map.of("success", true, "message", "Application deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/guarantors")
    public ResponseEntity<Map<String, Object>> getLoanGuarantors(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoanGuarantors(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 5. GOVERNANCE (SECRETARY, CHAIRPERSON, ASSEMBLY)
    // ========================================================================

    // âœ… SECRETARY: Table Loan (Opens Voting)
    @PostMapping("/{id}/table")
    public ResponseEntity<Map<String, Object>> tableLoanForMeeting(@PathVariable UUID id, @RequestParam String meetingDate) {
        try {
            loanService.tableLoan(id, LocalDate.parse(meetingDate));
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan added to Meeting Agenda"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // âœ… CHAIRPERSON: Start Voting
    @PostMapping("/{id}/start-voting")
    public ResponseEntity<Map<String, Object>> startVoting(@PathVariable UUID id) {
        try {
            loanService.openVoting(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voting Floor Opened"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // âœ… NEW: Members/Committee Voting
    @GetMapping("/agenda/active")
    public ResponseEntity<Map<String, Object>> getActiveVotingAgenda() {
        try {
            // Get logged-in user
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Use the SMART service method that filters out loans I've already voted on
            List<LoanDTO> myAgenda = loanService.getVotingAgendaForUser(user.getId());

            return ResponseEntity.ok(Map.of("success", true, "data", myAgenda));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Map<String, Object>> castVote(
            @PathVariable UUID id,
            @RequestParam boolean voteYes) {
        try {
            // Get logged-in user to check for conflict of interest
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Pass user ID to service to enforce "No Self-Voting" rule
            loanService.castVote(id, voteYes, user.getId());

            return ResponseEntity.ok(Map.of("success", true, "message", "Vote Cast Successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // âœ… SECRETARY: Finalize (Close Voting)
    @PostMapping("/{id}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeVote(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean approved, // Changed to Boolean (wrapper) to allow nulls
            @RequestParam(required = false) String comments) {
        try {
            // âœ… FIX: Changed method name from 'secretaryFinalize' to 'finalizeVote'
            loanService.finalizeVote(id, approved, comments != null ? comments : "General Assembly Decision");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Voting finalized successfully."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ ADMIN: Final System Approval
    @PostMapping("/{id}/admin-approve")
    public ResponseEntity<Map<String, Object>> adminApprove(@PathVariable UUID id) {
        try {
            loanService.adminApprove(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Final Approval Granted. Sent to Treasurer."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 🚀 POWER FEATURES: Advanced Loan Calculations
    // ========================================================================

    /**
     * Calculate loan payment schedule (amortization)
     */
    @GetMapping("/calculator/schedule")
    public ResponseEntity<Map<String, Object>> calculateSchedule(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal interestRate,
            @RequestParam Integer months) {
        try {
            List<LoanCalculatorService.PaymentScheduleItem> schedule =
                    loanCalculatorService.generateAmortizationSchedule(
                            principal, interestRate, months, LocalDate.now());

            BigDecimal totalInterest = loanCalculatorService.calculateTotalInterest(principal, interestRate, months);
            BigDecimal monthlyPayment = loanCalculatorService.calculateMonthlyPayment(principal, interestRate, months);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "schedule", schedule,
                            "monthlyPayment", monthlyPayment,
                            "totalInterest", totalInterest,
                            "totalPayment", monthlyPayment.multiply(BigDecimal.valueOf(months))
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Compare different loan term options
     */
    @GetMapping("/calculator/compare")
    public ResponseEntity<Map<String, Object>> compareLoanOptions(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal interestRate,
            @RequestParam List<Integer> terms) {
        try {
            List<Map<String, Object>> comparisons =
                    loanCalculatorService.compareLoanOptions(principal, interestRate, terms);

            return ResponseEntity.ok(Map.of("success", true, "data", comparisons));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate affordability (what member can borrow)
     */
    @GetMapping("/calculator/affordability")
    public ResponseEntity<Map<String, Object>> calculateAffordability(
            @RequestParam BigDecimal monthlyIncome,
            @RequestParam BigDecimal existingObligations,
            @RequestParam(defaultValue = "40") BigDecimal maxDebtRatio,
            @RequestParam BigDecimal interestRate,
            @RequestParam Integer months) {
        try {
            Map<String, Object> affordability = loanCalculatorService.calculateAffordability(
                    monthlyIncome, existingObligations, maxDebtRatio, interestRate, months);

            return ResponseEntity.ok(Map.of("success", true, "data", affordability));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Calculate early repayment amount
     */
    @GetMapping("/{id}/early-repayment")
    public ResponseEntity<Map<String, Object>> calculateEarlyRepayment(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "2") BigDecimal penaltyRate) {
        try {
            // Get loan from repository directly
            com.sacco.sacco_system.modules.loan.domain.entity.Loan loan =
                    loanService.getLoanRepository().findById(id)
                            .orElseThrow(() -> new RuntimeException("Loan not found"));

            Map<String, BigDecimal> calculation = loanCalculatorService.calculateEarlyRepayment(
                    loan, LocalDate.now(), penaltyRate);

            return ResponseEntity.ok(Map.of("success", true, "data", calculation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 🤖 POWER FEATURES: Automation & Scheduled Tasks
    // ========================================================================

    /**
     * Manually trigger interest calculation (ADMIN only)
     */
    @PostMapping("/automation/calculate-interest")
    public ResponseEntity<Map<String, Object>> manualInterestCalculation() {
        try {
            Map<String, Object> result = loanAutomationService.manualInterestCalculation(LocalDate.now());
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get automation status
     */
    @GetMapping("/automation/status")
    public ResponseEntity<Map<String, Object>> getAutomationStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("dailyInterestCalculation", "Scheduled at 2:00 AM daily");
            status.put("overdueCheck", "Scheduled at 3:00 AM daily");
            status.put("monthlyStatements", "Scheduled at 4:00 AM on 1st of month");
            status.put("paymentReminders", "Scheduled at 8:00 AM daily");
            status.put("status", "ACTIVE");

            return ResponseEntity.ok(Map.of("success", true, "data", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}








