package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.LoanProduct;
import com.sacco.sacco_system.entity.LoanRepayment;
import com.sacco.sacco_system.repository.LoanProductRepository;
import com.sacco.sacco_system.service.AuditService; // ✅ Import AuditService
import com.sacco.sacco_system.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final AuditService auditService; // ✅ Inject AuditService

    // ========================================================================
    // 1. LOAN PRODUCTS (Configuration)
    // ========================================================================

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        List<LoanProduct> products = loanProductRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody LoanProduct product) {
        try {
            LoanProduct saved = loanProductRepository.save(product);

            // ✅ LOG AUDIT: Record that a new product was created
            auditService.logAction(
                    "CREATE_PRODUCT",
                    "LoanProduct",
                    saved.getId().toString(),
                    "Created Loan Product: " + saved.getName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Loan Product Created Successfully");
            response.put("data", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 2. LOAN APPLICATION & LIFECYCLE
    // ========================================================================

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyForLoan(
            @RequestParam UUID memberId,
            @RequestParam UUID productId,
            @RequestParam BigDecimal amount,
            @RequestParam Integer duration) {
        try {
            LoanDTO loan = loanService.applyForLoan(memberId, productId, amount, duration);

            // ✅ LOG AUDIT: Loan Application
            auditService.logAction(
                    "APPLY_LOAN",
                    "Loan",
                    loan.getId().toString(),
                    "Loan Application: " + loan.getLoanNumber() + " for " + loan.getMemberName()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            response.put("message", "Loan application submitted successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.approveLoan(id);
            // Audit logging is already handled inside LoanService.approveLoan()
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Approved", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<Map<String, Object>> disburseLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.disburseLoan(id);
            // Audit logging is already handled inside LoanService.disburseLoan()
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Disbursed", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.rejectLoan(id);
            // Audit logging is already handled inside LoanService.rejectLoan()
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Rejected", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/write-off")
    public ResponseEntity<Map<String, Object>> writeOffLoan(@PathVariable UUID id, @RequestParam String reason) {
        try {
            loanService.writeOffLoan(id, reason);
            // Audit logging is already handled inside LoanService.writeOffLoan()
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Written Off successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/restructure")
    public ResponseEntity<Map<String, Object>> restructureLoan(@PathVariable UUID id, @RequestParam Integer newDuration) {
        try {
            LoanDTO loan = loanService.restructureLoan(id, newDuration);
            // ✅ LOG AUDIT: Restructure
            auditService.logAction(
                    "RESTRUCTURE_LOAN",
                    "Loan",
                    id.toString(),
                    "Loan Restructured. New Duration: " + newDuration + " months"
            );
            return ResponseEntity.ok(Map.of("success", true, "message", "Loan Restructured successfully", "data", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 3. GUARANTORS & REPAYMENT
    // ========================================================================

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

    @PostMapping("/{id}/repay")
    public ResponseEntity<Map<String, Object>> repayLoan(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        try {
            LoanRepayment repayment = loanService.repayLoan(id, amount);
            // Transaction logging happens in LoanService, which serves as an operational audit trail.
            // You can add explicit AuditService logging here if you want high-level admin logs for repayments too.
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Repayment processed successfully");
            response.put("remainingLoanBalance", repayment.getLoan().getLoanBalance());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========================================================================
    // 4. QUERIES & REPORTS
    // ========================================================================

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
}