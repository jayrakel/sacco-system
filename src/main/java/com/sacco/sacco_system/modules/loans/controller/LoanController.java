package com.sacco.sacco_system.modules.loans.controller;

import com.sacco.sacco_system.modules.loans.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loans.dto.LoanDTO;
import com.sacco.sacco_system.modules.loans.model.LoanProduct;
import com.sacco.sacco_system.modules.members.model.Member;
import com.sacco.sacco_system.modules.loans.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.members.repository.MemberRepository;
import com.sacco.sacco_system.modules.auth.repository.UserRepository;
import com.sacco.sacco_system.modules.system.service.AuditService;
import com.sacco.sacco_system.modules.loans.service.LoanService;
import com.sacco.sacco_system.modules.loans.service.LoanLimitService;
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
    private final LoanProductRepository loanProductRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    // ✅ FIX: Inject UserRepository to resolve the error
    private final UserRepository userRepository;

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
            auditService.logAction("CREATE_PRODUCT", "LoanProduct", saved.getId().toString(), "Created: " + saved.getName());
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
            auditService.logAction("RESTRUCTURE_LOAN", "Loan", id.toString(), "New Duration: " + newDuration);
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
            auditService.logAction("DELETE_LOAN", "Loan", id.toString(), "Application Deleted");
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

    // ✅ SECRETARY: Table Loan (Opens Voting)
    @PostMapping("/{id}/table")
    public ResponseEntity<Map<String, Object>> tableLoanForMeeting(@PathVariable UUID id, @RequestParam String meetingDate) {
        try {
            loanService.tableLoan(id, LocalDate.parse(meetingDate));
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan added to Meeting Agenda"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ CHAIRPERSON: Start Voting
    @PostMapping("/{id}/start-voting")
    public ResponseEntity<Map<String, Object>> startVoting(@PathVariable UUID id) {
        try {
            loanService.openVoting(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voting Floor Opened"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Members/Committee Voting
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

    // ✅ SECRETARY: Finalize (Close Voting)
    @PostMapping("/{id}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeVote(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean approved, // Changed to Boolean (wrapper) to allow nulls
            @RequestParam(required = false) String comments) {
        try {
            // ✅ FIX: Changed method name from 'secretaryFinalize' to 'finalizeVote'
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
}