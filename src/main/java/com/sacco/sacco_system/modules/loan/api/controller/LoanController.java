package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanLimitService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanLimitService loanLimitService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;

    // --- PHASE 1: ELIGIBILITY & INITIATION ---

    @GetMapping("/eligibility/check")
    public ResponseEntity<?> checkEligibility() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.checkEligibility(getCurrentUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/limits/check")
    public ResponseEntity<?> checkLimit() {
        try {
            UUID userId = getCurrentUserId();
            Member member = memberRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Member record not found for this user."));

            BigDecimal limit = loanLimitService.calculateMemberLoanLimit(member);
            return ResponseEntity.ok(Map.of("success", true, "limit", limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/initiate-with-fee")
    public ResponseEntity<?> initiateWithFee(@RequestBody Map<String, Object> payload) {
        try {
            UUID productId = UUID.fromString(payload.get("productId").toString());
            String reference = payload.get("referenceCode").toString();
            String paymentMethod = payload.getOrDefault("paymentMethod", "MPESA").toString();
            String externalRef = payload.getOrDefault("externalReference", reference).toString();

            LoanDTO draft = loanService.initiateWithFee(getCurrentUserId(), productId, externalRef, paymentMethod);
            return ResponseEntity.ok(Map.of("success", true, "data", draft));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> submitDetails(@RequestBody Map<String, Object> payload) {
        try {
            UUID loanId = UUID.fromString(payload.get("loanId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            Integer duration = Integer.parseInt(payload.get("duration").toString());
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.submitApplication(loanId, amount, duration)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 2: GUARANTORS ---

    @GetMapping("/{id}/guarantors")
    public ResponseEntity<?> getLoanGuarantors(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getGuarantorsByLoan(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/guarantors")
    public ResponseEntity<?> addGuarantor(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        try {
            UUID memberId = UUID.fromString(payload.get("memberId").toString());
            BigDecimal amount = new BigDecimal(payload.get("guaranteeAmount").toString());
            GuarantorDTO guarantor = loanService.addGuarantor(id, memberId, amount);
            return ResponseEntity.ok(Map.of("success", true, "data", guarantor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-requests")
    public ResponseEntity<?> sendRequests(@PathVariable UUID id) {
        try {
            loanService.finalizeGuarantorRequests(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Requests sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- QUERIES ---

    @GetMapping("/guarantors/eligible")
    public ResponseEntity<?> getEligibleGuarantors() {
        try {
            UUID userId = getCurrentUserId();
            Member member = memberRepository.findByUserId(userId).orElseThrow(() -> new RuntimeException("Member not found"));

            List<Member> eligibleMembers = loanService.getEligibleGuarantors(member.getId());

            List<Map<String, Object>> response = eligibleMembers.stream().map(m -> Map.<String, Object>of(
                    "id", m.getId(),
                    "firstName", m.getFirstName(),
                    "lastName", m.getLastName(),
                    "memberNumber", m.getMemberNumber()
            )).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("success", true, "data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/guarantors/requests")
    public ResponseEntity<?> getGuarantorRequests() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getGuarantorRequests(getCurrentUserId())));
    }

    @PostMapping("/guarantors/{requestId}/respond")
    public ResponseEntity<?> respondToRequest(@PathVariable UUID requestId, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            UUID userId = getCurrentUserId();

            loanService.respondToGuarantorRequest(userId, requestId, status);

            return ResponseEntity.ok(Map.of("success", true, "message", "Response recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 3: ADMIN GOVERNANCE & DISBURSEMENT ---

    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingLoans() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getPendingLoansForAdmin()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/{loanId}/review")
    public ResponseEntity<?> reviewLoan(@PathVariable UUID loanId, @RequestBody Map<String, String> payload) {
        try {
            String decision = payload.get("decision");
            String remarks = payload.getOrDefault("remarks", "");
            UUID adminId = getCurrentUserId();

            loanService.reviewLoanApplication(adminId, loanId, decision, remarks);

            return ResponseEntity.ok(Map.of("success", true, "message", "Loan review processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- GOVERNANCE ENDPOINTS ---

    @PostMapping("/secretary/{loanId}/table")
    public ResponseEntity<?> tableLoan(@PathVariable UUID loanId, @RequestBody Map<String, String> payload) {
        try {
            // ✅ UPDATED: Parse LocalDateTime for exact scheduling
            LocalDateTime dateTime = LocalDateTime.parse(payload.get("meetingDate"));
            loanService.tableLoanForMeeting(loanId, dateTime);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan tabled for meeting successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/chairperson/{loanId}/start-voting")
    public ResponseEntity<?> startVoting(@PathVariable UUID loanId) {
        try {
            loanService.startVoting(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voting is now open for this loan."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Member Vote Casting
    @PostMapping("/{loanId}/vote")
    public ResponseEntity<?> castVote(@PathVariable UUID loanId, @RequestBody Map<String, Boolean> payload) {
        try {
            boolean voteYes = payload.get("vote");
            loanService.castVote(loanId, getCurrentUserId(), voteYes);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vote cast successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Get active votes for member (Filters own loans)
    @GetMapping("/voting/active")
    public ResponseEntity<?> getActiveVotes() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getActiveVotesForMember(getCurrentUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ UPDATED: Secretary Finalizes the Vote
    @PostMapping("/secretary/{loanId}/finalize-vote")
    public ResponseEntity<?> finalizeVote(@PathVariable UUID loanId, @RequestBody Map<String, Object> payload) {
        try {
            boolean approved = Boolean.parseBoolean(payload.get("approved").toString());
            String minutes = payload.getOrDefault("minutes", "").toString();
            loanService.closeVoting(loanId, approved, minutes);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vote Finalized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Chairperson Final Approval
    @PostMapping("/chairperson/{loanId}/final-approval")
    public ResponseEntity<?> chairpersonFinalApprove(@PathVariable UUID loanId) {
        try {
            loanService.chairpersonFinalApprove(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Approved for Disbursement"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Disburse Loan Funds
     * Moves money from Sacco accounts to Member Savings
     */
    @PostMapping("/admin/{loanId}/disburse")
    public ResponseEntity<?> disburseLoan(@PathVariable UUID loanId) {
        try {
            loanService.disburseLoan(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan disbursed successfully. Funds moved to Member account."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- SHARED HELPERS ---

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoansByMember(getCurrentUserId())));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanProductRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoanById(id)));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user session not found"));
    }
}