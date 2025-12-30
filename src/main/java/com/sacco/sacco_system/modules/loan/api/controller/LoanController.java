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

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanEligibilityService eligibilityService;
    private final LoanApplicationService applicationService;
    private final LoanReadService readService;
    private final LoanProductService productService;
    private final UserService userService;

    @GetMapping("/eligibility/check")
    public ResponseEntity<ApiResponse<Object>> checkEligibility(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Explicitly cast or wrap the result to ensure type safety
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Checked", eligibilityService.checkEligibility(user.getId())));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Object>> apply(@AuthenticationPrincipal UserDetails userDetails, @RequestBody LoanRequestDTO req) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        // ✅ Explicitly use <Object> to stop inference errors
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Applied", applicationService.createDraft(user.getId(), req)));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<Object>> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Fetched", readService.getMemberLoans(user.getId())));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Object>> getProducts() {
        return ResponseEntity.ok(new ApiResponse<Object>(true, "Fetched", productService.getAllActiveProducts()));
    }
}