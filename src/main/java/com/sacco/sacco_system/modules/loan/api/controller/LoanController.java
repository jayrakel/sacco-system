package com.sacco.sacco_system.modules.loan.api.controller;

import com.sacco.sacco_system.modules.loan.api.dto.GuarantorDTO;
import com.sacco.sacco_system.modules.loan.api.dto.LoanDTO;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanProductRepository;
import com.sacco.sacco_system.modules.loan.domain.service.LoanService;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import com.sacco.sacco_system.modules.users.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final LoanProductRepository loanProductRepository;

    // --- PHASE 1: ELIGIBILITY & INITIATION ---
    @GetMapping("/eligibility/check")
    public ResponseEntity<?> checkEligibility() {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.checkEligibility(getCurrentMemberId())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/initiate-with-fee")
    public ResponseEntity<?> initiateWithFee(@RequestBody Map<String, Object> payload) {
        try {
            UUID productId = UUID.fromString(payload.get("productId").toString());
            String reference = payload.get("referenceCode").toString();
            // ✅ Extract Payment Method (Defaults to MPESA if missing)
            String paymentMethod = payload.getOrDefault("paymentMethod", "MPESA").toString();

            LoanDTO draft = loanService.initiateWithFee(getCurrentMemberId(), productId, reference, paymentMethod);
            return ResponseEntity.ok(Map.of("success", true, "data", draft));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/apply")
    public ResponseEntity<?> submitDetails(@RequestBody Map<String, Object> payload) {
        try {
            UUID loanId = UUID.fromString(payload.get("loanId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            Integer duration = Integer.parseInt(payload.get("duration").toString());
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.submitApplication(loanId, amount, duration)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- PHASE 2: GUARANTORS ---

    @GetMapping("/{id}/guarantors")
    public ResponseEntity<?> getLoanGuarantors(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", loanService.getGuarantorsByLoan(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/guarantors")
    public ResponseEntity<?> addGuarantor(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        try {
            UUID memberId = UUID.fromString(payload.get("memberId").toString());
            BigDecimal amount = new BigDecimal(payload.get("guaranteeAmount").toString());
            GuarantorDTO guarantor = loanService.addGuarantor(id, memberId, amount);
            return ResponseEntity.ok(Map.of("success", true, "data", guarantor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/send-requests")
    public ResponseEntity<?> sendRequests(@PathVariable UUID id) {
        try {
            loanService.finalizeGuarantorRequests(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Requests sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- QUERIES ---

    @GetMapping("/guarantors/eligible")
    public ResponseEntity<?> getEligibleGuarantors() {
        try {
            UUID applicantId = getCurrentMemberId();
            List<Member> eligibleMembers = loanService.getEligibleGuarantors(applicantId);

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
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getGuarantorRequests(getCurrentMemberId())));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<?> getMyLoans() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoansByMember(getCurrentMemberId())));
    }

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(Map.of("success", true, "data", loanProductRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", loanService.getLoanById(id)));
    }

    private UUID getCurrentMemberId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrOfficialEmail(email).map(User::getId).orElseThrow(() -> new RuntimeException("User not found"));
    }
}