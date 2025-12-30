package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.service.LoanApplicationService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanEligibilityService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanProductService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanReadService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanEligibilityService eligibilityService;
    private final LoanApplicationService applicationService;
    private final LoanReadService readService;
    private final LoanProductService productService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Object>> getLoanDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Pass Email instead of ID
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Dashboard fetched", readService.getMemberDashboard(user.getEmail())));
    }

    @GetMapping("/eligibility/check")
    public ResponseEntity<ApiResponse<Object>> checkEligibility(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Pass Email
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Checked", eligibilityService.checkEligibility(user.getEmail())));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Object>> apply(@AuthenticationPrincipal UserDetails userDetails, @RequestBody LoanRequestDTO req) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Pass Email
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Draft Created", applicationService.createDraft(user.getEmail(), req)));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<Object>> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Pass Email
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Fetched", readService.getMemberLoans(user.getEmail())));
    }

    // ... (Keep getProducts, addGuarantor, submitApplication as is) ...
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Object>> getProducts() {
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Fetched", productService.getAllActiveProducts()));
    }

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<ApiResponse<Object>> addGuarantor(@PathVariable UUID loanId, @RequestBody Map<String, Object> body) {
        UUID guarantorId = UUID.fromString((String) body.get("guarantorMemberId"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        applicationService.addGuarantor(loanId, guarantorId, amount);
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Guarantor Added"));
    }

    @PostMapping("/{loanId}/submit")
    public ResponseEntity<ApiResponse<Object>> submitApplication(@PathVariable UUID loanId) {
        applicationService.submitApplication(loanId);
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Submitted"));
    }
}