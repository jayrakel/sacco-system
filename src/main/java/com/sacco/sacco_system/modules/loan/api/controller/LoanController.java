package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;

    // ========================================================================
    // 1. APPLICATION & GUARANTORS
    // ========================================================================

    @PostMapping("/initiate-with-fee")
    public ResponseEntity<?> initiateWithFee(@RequestParam UUID productId, @RequestParam String reference) {
        try {
            UUID memberId = getCurrentMemberId();
            LoanDTO draft = loanService.initiateWithFee(memberId, productId, reference);
            return ResponseEntity.ok(Map.of("success", true, "data", draft));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestParam UUID loanId, @RequestParam BigDecimal amount,
            @RequestParam Integer duration) {
        try {
            LoanDTO updatedLoan = loanService.submitApplication(loanId, amount, duration);
            return ResponseEntity.ok(Map.of("success", true, "data", updatedLoan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<?> addGuarantor(@PathVariable UUID loanId, @RequestBody Map<String, Object> payload) {
        try {
            UUID guarantorMemberId = UUID.fromString(payload.get("memberId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            return ResponseEntity
                    .ok(Map.of("success", true, "data", loanService.addGuarantor(loanId, guarantorMemberId, amount)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 2. GOVERNANCE & REVIEW
    // ========================================================================

    @PostMapping("/{id}/submit-review")
    public ResponseEntity<?> submitForReview(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.submitToLoanOfficer(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/officer-approve")
    public ResponseEntity<?> officerApprove(@PathVariable UUID id, @RequestBody Map<String, String> payload) {
        try {
            loanService.officerApprove(id, payload.get("comments"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Forwarded to Secretary"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/table")
    public ResponseEntity<?> tableLoan(@PathVariable UUID id, @RequestParam String meetingDate) {
        try {
            loanService.tableLoanOnAgenda(id, LocalDate.parse(meetingDate));
            return ResponseEntity.ok(Map.of("success", true, "message", "Added to Agenda"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start-voting")
    public ResponseEntity<?> startVoting(@PathVariable UUID id) {
        try {
            loanService.openVoting(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Voting Open"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<?> castVote(@PathVariable UUID id, @RequestParam boolean voteYes) {
        try {
            loanService.castVote(id, getCurrentMemberId(), voteYes);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<?> finalizeVote(@PathVariable UUID id, @RequestParam(required = false) Boolean approved,
            @RequestParam String comments) {
        try {
            loanService.finalizeLoanDecision(id, approved, comments);
            return ResponseEntity.ok(Map.of("success", true, "message", "Decision Finalized"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 3. QUERIES
    // ========================================================================

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoansByMember(getCurrentMemberId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoanById(id)));
    }

    private UUID getCurrentMemberId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * ✅ STEP 8: Final Disbursement (Treasurer)
     * Location: LoanController.java
     */
    @PostMapping("/{id}/disburse")
    public ResponseEntity<?> disburse(@PathVariable UUID id, @RequestParam String reference) {
        try {
            LoanDTO disbursed = loanService.disburseLoan(id, reference);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Loan activated and funds disbursed.",
                    "data", disbursed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}