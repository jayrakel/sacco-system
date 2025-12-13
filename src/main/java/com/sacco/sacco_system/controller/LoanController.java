package com.sacco.sacco_system.controller;

import com.sacco.sacco_system.dto.GuarantorDTO;
import com.sacco.sacco_system.dto.LoanDTO;
import com.sacco.sacco_system.entity.Loan;
import com.sacco.sacco_system.entity.LoanRepayment;
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
    
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyForLoan(
            @RequestParam UUID memberId,
            @RequestParam BigDecimal principalAmount,
            @RequestParam BigDecimal interestRate,
            @RequestParam Integer durationMonths) {
        try {
            LoanDTO loan = loanService.applyForLoan(memberId, principalAmount, interestRate, durationMonths);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            response.put("message", "Loan application submitted successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<Map<String, Object>> addGuarantor(
            @PathVariable UUID loanId,
            @RequestBody GuarantorDTO request) {
        try {
            GuarantorDTO responseDTO = loanService.addGuarantor(
                loanId, 
                request.getMemberId(), 
                request.getGuaranteeAmount()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", responseDTO);
            response.put("message", "Guarantor added successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLoanById(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.getLoanById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @GetMapping("/number/{loanNumber}")
    public ResponseEntity<Map<String, Object>> getLoanByNumber(@PathVariable String loanNumber) {
        try {
            LoanDTO loan = loanService.getLoanByNumber(loanNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<Map<String, Object>> repayLoan(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        try {
            LoanRepayment repayment = loanService.repayLoan(id, amount);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Repayment processed successfully");
            response.put("installmentNumber", repayment.getRepaymentNumber());
            response.put("remainingLoanBalance", repayment.getLoan().getLoanBalance());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getLoansByMemberId(@PathVariable UUID memberId) {
        List<LoanDTO> loans = loanService.getLoansByMemberId(memberId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", loans);
        response.put("count", loans.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLoans() {
        List<LoanDTO> loans = loanService.getAllLoans();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", loans);
        response.put("count", loans.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getLoansByStatus(@PathVariable String status) {
        try {
            Loan.LoanStatus loanStatus = Loan.LoanStatus.valueOf(status.toUpperCase());
            List<LoanDTO> loans = loanService.getLoansByStatus(loanStatus);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loans);
            response.put("count", loans.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.approveLoan(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            response.put("message", "Loan approved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}/disburse")
    public ResponseEntity<Map<String, Object>> disburseLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.disburseLoan(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            response.put("message", "Loan disbursed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectLoan(@PathVariable UUID id) {
        try {
            LoanDTO loan = loanService.rejectLoan(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", loan);
            response.put("message", "Loan rejected successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/totals/disbursed")
    public ResponseEntity<Map<String, Object>> getTotalDisbursed() {
        BigDecimal total = loanService.getTotalDisbursedLoans();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalDisbursed", total);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/totals/outstanding")
    public ResponseEntity<Map<String, Object>> getTotalOutstanding() {
        BigDecimal total = loanService.getTotalOutstandingLoans();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalOutstanding", total);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/totals/interest")
    public ResponseEntity<Map<String, Object>> getTotalInterest() {
        BigDecimal total = loanService.getTotalInterestCollected();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalInterest", total);
        return ResponseEntity.ok(response);
    }
}
