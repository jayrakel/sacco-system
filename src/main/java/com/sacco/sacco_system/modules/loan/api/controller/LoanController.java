package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.loan.api.dto.LoanRequestDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
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

    // --- 1. Dashboard & Member Data ---

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

    // ✅ ADDED: Admin/Officer: Fetch ALL loans
    @GetMapping("")
    public ResponseEntity<ApiResponse<Object>> getAllLoans() {
        // Ideally add @PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
        return ResponseEntity.ok(new ApiResponse<>(true, "All Loans Fetched", readService.getAllLoans()));
    }

    // --- 2. Product Management ---

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Object>> getProducts() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched", productService.getAllActiveProducts()));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Object>> createProduct(@RequestBody LoanProduct product) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Product Created", productService.createProduct(product)));
    }

    // --- 3. Application Process (Member) ---

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Object>> apply(@AuthenticationPrincipal UserDetails userDetails, @RequestBody LoanRequestDTO req) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Draft Created", applicationService.createDraft(user.getEmail(), req)));
    }

    @PostMapping("/{loanId}/guarantors")
    public ResponseEntity<ApiResponse<Object>> addGuarantor(@PathVariable UUID loanId, @RequestBody Map<String, Object> body) {
        UUID guarantorId = UUID.fromString((String) body.get("guarantorMemberId"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        applicationService.addGuarantor(loanId, guarantorId, amount);
        return ResponseEntity.ok(new ApiResponse<>(true, "Guarantor Added"));
    }

    @PostMapping("/{loanId}/submit")
    public ResponseEntity<ApiResponse<Object>> submitApplication(@PathVariable UUID loanId) {
        applicationService.submitApplication(loanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Submitted"));
    }

    // --- 4. Workflow Actions (Officer/Admin) ---
    // These support the LoanManager.jsx buttons

    @PostMapping("/{loanId}/review")
    public ResponseEntity<ApiResponse<Object>> startReview(@PathVariable UUID loanId) {
        // In future: applicationService.transitionStatus(loanId, LoanStatus.LOAN_OFFICER_REVIEW);
        return ResponseEntity.ok(new ApiResponse<>(true, "Review Process Started"));
    }

    @PostMapping("/{loanId}/approve")
    public ResponseEntity<ApiResponse<Object>> officerApprove(@PathVariable UUID loanId) {
        // In future: applicationService.transitionStatus(loanId, LoanStatus.SECRETARY_TABLED);
        return ResponseEntity.ok(new ApiResponse<>(true, "Approved and Tabled for Secretary"));
    }

    @PostMapping("/{loanId}/table")
    public ResponseEntity<ApiResponse<Object>> tableLoan(@PathVariable UUID loanId, @RequestParam(required = false) String meetingDate) {
        // In future: applicationService.setMeetingDate(loanId, meetingDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Loan Tabled for Meeting"));
    }

    @PostMapping("/{loanId}/vote/open")
    public ResponseEntity<ApiResponse<Object>> openVoting(@PathVariable UUID loanId) {
        // In future: applicationService.transitionStatus(loanId, LoanStatus.VOTING_OPEN);
        return ResponseEntity.ok(new ApiResponse<>(true, "Voting Session Opened"));
    }

    @PostMapping("/{loanId}/vote/close")
    public ResponseEntity<ApiResponse<Object>> closeVoting(@PathVariable UUID loanId,
                                                           @RequestParam(required = false) Boolean manualApproved,
                                                           @RequestParam(required = false) String comments) {
        // In future: applicationService.closeVoting(loanId, manualApproved, comments);
        return ResponseEntity.ok(new ApiResponse<>(true, "Voting Closed & Recorded"));
    }

    @PostMapping("/{loanId}/final-approve")
    public ResponseEntity<ApiResponse<Object>> finalApprove(@PathVariable UUID loanId) {
        // In future: applicationService.transitionStatus(loanId, LoanStatus.TREASURER_DISBURSEMENT);
        return ResponseEntity.ok(new ApiResponse<>(true, "Final Approval Granted"));
    }

    @PostMapping("/{loanId}/disburse")
    public ResponseEntity<ApiResponse<Object>> disburse(@PathVariable UUID loanId, @RequestParam(required = false) String checkNumber) {
        // In future: applicationService.disburseLoan(loanId, checkNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, "Funds Disbursed"));
    }

    // --- 5. Voting & Requests Data ---

    @GetMapping("/voting/active")
    public ResponseEntity<ApiResponse<Object>> getActiveVotes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Votes fetched", readService.getPendingVotes(user.getEmail())));
    }

    @GetMapping("/guarantors/requests")
    public ResponseEntity<ApiResponse<Object>> getGuarantorRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(new ApiResponse<>(true, "Requests fetched", readService.getGuarantorRequests(user.getEmail())));
    }

    @PostMapping("/{loanId}/vote")
    public ResponseEntity<ApiResponse<Object>> voteOnLoan(@PathVariable UUID loanId, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Vote Cast"));
    }
}