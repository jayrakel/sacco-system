package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanProduct;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final LoanProductRepository loanProductRepository;

    // --- PHASE 1: ELIGIBILITY ---

    /**
     * Checks if the member meets prerequisites (savings, duration, no active loans)
     */
    @GetMapping("/eligibility/check")
    public ResponseEntity<?> checkEligibility() {
        try {
            UUID userId = getCurrentMemberId(); // Renamed from getCurrentUserId to match logic
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", loanService.checkEligibility(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 2: FEE PAYMENT & INITIATION ---

    /**
     * Processes the application fee and creates a DRAFT loan record
     */
    @PostMapping("/initiate-with-fee")
    public ResponseEntity<?> initiateWithFee(@RequestBody Map<String, Object> payload) {
        try {
            // Extracted from JSON body to match Axios post structure
            UUID productId = UUID.fromString(payload.get("productId").toString());
            String reference = payload.get("referenceCode").toString();

            UUID memberId = getCurrentMemberId();
            LoanDTO draft = loanService.initiateWithFee(memberId, productId, reference);

            return ResponseEntity.ok(Map.of("success", true, "data", draft));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 3: SUBMITTING DETAILS ---

    /**
     * Submits specific loan amount and duration details
     */
    @PostMapping("/apply")
    public ResponseEntity<?> submitDetails(@RequestBody Map<String, Object> payload) {
        try {
            UUID loanId = UUID.fromString(payload.get("loanId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            Integer duration = Integer.parseInt(payload.get("duration").toString());

            LoanDTO updatedLoan = loanService.submitApplication(loanId, amount, duration);
            return ResponseEntity.ok(Map.of("success", true, "data", updatedLoan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- QUERIES ---

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", loanService.getLoansByMember(getCurrentMemberId())
        ));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", loanProductRepository.findAll()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", loanService.getLoanById(id)
        ));
    }

    // --- HELPERS ---

    /**
     * Standardized helper to retrieve the UUID of the logged-in user
     */
    private UUID getCurrentMemberId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }
}