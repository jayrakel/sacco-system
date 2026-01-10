package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanApplicationDraft;
import com.sacco.sacco_system.modules.loan.domain.service.LoanApplicationService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanEligibilityService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanProductService;
import com.sacco.sacco_system.modules.loan.domain.service.LoanReadService;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    // --- DRAFT PROCESS ---

    @GetMapping("/draft")
    public ResponseEntity<ApiResponse<LoanApplicationDraft>> getCurrentDraft(@AuthenticationPrincipal UserDetails userDetails) {
        return applicationService.getCurrentDraft(userDetails.getUsername())
                .map(draft -> ResponseEntity.ok(new ApiResponse<>(true, "Active Draft Found", draft)))
                .orElse(ResponseEntity.ok(new ApiResponse<>(true, "No Active Draft", null)));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<LoanApplicationDraft>> startApplication(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        LoanApplicationDraft draft = applicationService.startApplication(user.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Application Started", draft));
    }

    @PostMapping("/drafts/{draftId}/pay-fee")
    public ResponseEntity<ApiResponse<LoanApplicationDraft>> confirmDraftFee(
            @PathVariable UUID draftId,
            @RequestBody Map<String, String> payload) {
        String ref = payload.get("referenceCode");
        LoanApplicationDraft draft = applicationService.confirmDraftFee(draftId, ref);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fee Confirmed", draft));
    }

    // ✅ FIXED: Now calls the service method that returns 'totalDeposits'
    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberLimits(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> limits = eligibilityService.getLoanLimits(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(true, "Limits Calculated", limits));
    }

    @PostMapping("/drafts/{draftId}/submit-details")
    public ResponseEntity<ApiResponse<Loan>> submitLoanDetails(
            @PathVariable UUID draftId,
            @RequestBody LoanRequestDTO request) {
        Loan loan = applicationService.createLoanFromDraft(draftId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan Details Submitted", loan));
    }

    // --- 4. REAL LOAN PROCESS ---

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<ApiResponse<Object>> addGuarantor(@PathVariable UUID loanId, @RequestBody Map<String, Object> body) {
        String memberIdStr = (String) body.get("memberId");
        if (memberIdStr == null) memberIdStr = (String) body.get("guarantorMemberId");
        if (memberIdStr == null) throw new IllegalArgumentException("Member ID is required");

        UUID guarantorId = UUID.fromString(memberIdStr);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        applicationService.addGuarantor(loanId, guarantorId, amount);
        return ResponseEntity.ok(new ApiResponse<>(true, "Guarantor Added"));
    }

    // ✅ NEW ENDPOINT: Get Guarantors for a Loan (For Resume Application State)
    @GetMapping("/{loanId}/guarantors")
    public ResponseEntity<ApiResponse<Object>> getLoanGuarantors(@PathVariable UUID loanId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Guarantors fetched", readService.getLoanGuarantors(loanId)));
    }

    @PostMapping("/{loanId}/submit")
    public ResponseEntity<ApiResponse<Object>> submitApplication(@PathVariable UUID loanId) {
        applicationService.submitApplication(loanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Submitted"));
    }

    // --- READ & DASHBOARD ---
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Object>> getLoanDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard fetched", readService.getMemberDashboard(user.getEmail())));
    }

    @GetMapping("/eligibility/check")
    public ResponseEntity<ApiResponse<Object>> checkEligibility(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Checked", eligibilityService.checkEligibility(user.getEmail())));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<ApiResponse<Object>> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched", readService.getMemberLoans(user.getEmail())));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Object>> getProducts() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched", productService.getAllActiveProducts()));
    }

    @GetMapping("/voting/active")
    public ResponseEntity<ApiResponse<Object>> getActiveVotes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Votes fetched", readService.getPendingVotes(user.getEmail())));
    }
}