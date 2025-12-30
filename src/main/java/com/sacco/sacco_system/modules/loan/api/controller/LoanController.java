package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanGovernanceService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanLimitService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanOriginationService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanReadService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanOriginationService originationService;
    private final LoanGovernanceService governanceService;
    private final LoanReadService readService;
    private final LoanLimitService loanLimitService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final LoanProductRepository loanProductRepository;

    // --- PHASE 1: ELIGIBILITY & INITIATION ---

    @GetMapping("/eligibility/check")
    public ResponseEntity<?> checkEligibility() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", originationService.checkEligibility(getCurrentUserId())));
        } catch (Exception e) {
            log.error("Eligibility check failed", e);
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
            log.error("Limit check failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ FIXED: Robust input validation to prevent HTTP 400 Errors on null values
     */
    @PostMapping("/initiate-with-fee")
    public ResponseEntity<?> initiateWithFee(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Validate Payload Keys exist
            if (payload.get("productId") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product ID is missing"));
            }
            if (payload.get("referenceCode") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Payment Reference Code is missing"));
            }

            // 2. Parse Data Safely
            UUID productId;
            try {
                productId = UUID.fromString(payload.get("productId").toString());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid Product ID format"));
            }

            String reference = String.valueOf(payload.get("referenceCode"));
            String paymentMethod = payload.containsKey("paymentMethod") ? String.valueOf(payload.get("paymentMethod")) : "MPESA";

            // 3. Safe Fallback for External Reference (prevents NullPointerException)
            Object extRefObj = payload.get("externalReference");
            String externalRef = (extRefObj != null && !extRefObj.toString().isEmpty())
                    ? extRefObj.toString()
                    : reference;

            // 4. Call Service
            LoanDTO draft = originationService.initiateWithFee(getCurrentUserId(), productId, externalRef, paymentMethod);

            return ResponseEntity.ok(Map.of("success", true, "data", draft));

        } catch (Exception e) {
            log.error("Initiate loan failed", e); // Print full error to console
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> submitDetails(@RequestBody Map<String, Object> payload) {
        try {
            UUID loanId = UUID.fromString(payload.get("loanId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            Integer duration = Integer.parseInt(payload.get("duration").toString());

            return ResponseEntity.ok(Map.of("success", true, "data",
                    originationService.submitApplication(loanId, amount, duration)));
        } catch (Exception e) {
            log.error("Submit application failed", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 2: GUARANTORS ---

    @GetMapping("/{id}/guarantors")
    public ResponseEntity<?> getLoanGuarantors(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", readService.getGuarantorsByLoan(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/guarantors")
    public ResponseEntity<?> addGuarantor(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        try {
            UUID memberId = UUID.fromString(payload.get("memberId").toString());
            BigDecimal amount = new BigDecimal(payload.get("guaranteeAmount").toString());

            GuarantorDTO guarantor = originationService.addGuarantor(id, memberId, amount);
            return ResponseEntity.ok(Map.of("success", true, "data", guarantor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-requests")
    public ResponseEntity<?> sendRequests(@PathVariable UUID id) {
        try {
            originationService.finalizeGuarantorRequests(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Requests sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/guarantors/eligible")
    public ResponseEntity<?> getEligibleGuarantors() {
        try {
            List<Member> eligibleMembers = originationService.getEligibleGuarantors(getCurrentUserId());
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
        return ResponseEntity.ok(Map.of("success", true, "data", readService.getGuarantorRequests(getCurrentUserId())));
    }

    @PostMapping("/guarantors/{requestId}/respond")
    public ResponseEntity<?> respondToRequest(@PathVariable UUID requestId, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            originationService.respondToGuarantorRequest(getCurrentUserId(), requestId, status);
            return ResponseEntity.ok(Map.of("success", true, "message", "Response recorded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 3: GOVERNANCE ---

    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingLoans() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", readService.getPendingLoansForAdmin()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/{loanId}/review")
    public ResponseEntity<?> reviewLoan(@PathVariable UUID loanId, @RequestBody Map<String, String> payload) {
        try {
            governanceService.reviewApplication(loanId, payload.get("decision"), payload.getOrDefault("remarks", ""));
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan review processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/secretary/{loanId}/table")
    public ResponseEntity<?> tableLoan(@PathVariable UUID loanId, @RequestBody Map<String, String> payload) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(payload.get("meetingDate"));
            governanceService.tableLoan(loanId, dateTime);
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan tabled for meeting successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/chairperson/{loanId}/start-voting")
    public ResponseEntity<?> startVoting(@PathVariable UUID loanId) {
        try {
            governanceService.startVoting(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voting is now open."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{loanId}/vote")
    public ResponseEntity<?> castVote(@PathVariable UUID loanId, @RequestBody Map<String, Boolean> payload) {
        try {
            governanceService.castVote(loanId, getCurrentUserId(), payload.get("vote"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Vote cast successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> createLoanProduct(@RequestBody com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct product) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanProductRepository.save(product)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/voting/active")
    public ResponseEntity<?> getActiveVotes() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", readService.getActiveVotesForMember(getCurrentUserId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/secretary/{loanId}/finalize-vote")
    public ResponseEntity<?> finalizeVote(@PathVariable UUID loanId, @RequestBody Map<String, Object> payload) {
        try {
            governanceService.closeVoting(loanId, Boolean.parseBoolean(payload.get("approved").toString()), payload.getOrDefault("minutes", "").toString());
            return ResponseEntity.ok(Map.of("success", true, "message", "Vote Finalized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/chairperson/{loanId}/final-approval")
    public ResponseEntity<?> chairpersonFinalApprove(@PathVariable UUID loanId) {
        try {
            governanceService.chairpersonFinalApprove(loanId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Approved for Disbursement"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- SHARED HELPERS ---

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok(Map.of("success", true, "data", readService.getMemberLoans(getCurrentUserId())));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanProductRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", readService.getLoanById(id)));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user session not found"));
    }
}