package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.LoanProduct;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.LoanProductRepository;
import com.sacco.sacco_system.repository.MemberRepository;
import com.sacco.sacco_system.service.AuditService;
import com.sacco.sacco_system.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanProductRepository loanProductRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

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

    // ✅ STEP 1: CREATE DRAFT APPLICATION
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyForLoan(
            @RequestParam(required = false) UUID memberId,
            @RequestParam UUID productId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer duration,
            @RequestParam(defaultValue = "MONTHS") String durationUnit) {
        try {
            if (memberId == null) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                memberId = memberRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Member not found")).getId();
            }

            LoanDTO loan = loanService.initiateApplication(memberId, productId, amount, duration, durationUnit);
            auditService.logAction("INITIATE_APPLICATION", "Loan", loan.getId().toString(), "Draft Created: " + loan.getLoanNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Loan Draft Created", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ STEP 2: ADD GUARANTORS
    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<Map<String, Object>> addGuarantor(
            @PathVariable UUID loanId,
            @RequestBody GuarantorDTO request) {
        try {
            GuarantorDTO responseDTO = loanService.addGuarantor(loanId, request.getMemberId(), request.getGuaranteeAmount());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Guarantor added", "data", responseDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ STEP 3: SUBMIT TO GUARANTORS
    @PostMapping("/{id}/send-requests")
    public ResponseEntity<Map<String, Object>> sendGuarantorRequests(@PathVariable UUID id) {
        try {
            loanService.submitToGuarantors(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Requests sent to guarantors"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Get Pending Requests for Logged-in User (Moved Logic to Service)
    @GetMapping("/guarantors/requests")
    public ResponseEntity<Map<String, Object>> getMyGuarantorRequests() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Member not found"));

            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getPendingGuarantorRequests(member.getId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ NEW: Respond to Request (Moved Logic to Service)
    @PostMapping("/guarantors/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToGuarantorRequest(@PathVariable UUID id, @RequestParam boolean accepted) {
        try {
            loanService.respondToGuarantorship(id, accepted);
            return ResponseEntity.ok(Map.of("success", true, "message", accepted ? "Request Accepted" : "Request Declined"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ STEP 4: PAY FEE
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Approved", "data", loanService.approveLoan(id)));
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

    @GetMapping("/limits/check")
    public ResponseEntity<Map<String, Object>> checkLoanLimit() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        BigDecimal limit = loanService.calculateMemberLoanLimit(member);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "limit", limit,
                "savings", member.getTotalSavings()
        ));
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
}