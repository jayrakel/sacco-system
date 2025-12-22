package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.ShareCapital;
import com.sacco.sacco_system.modules.finance.domain.service.ShareCapitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Share Capital Controller
 * Manages member share purchases and tracking
 */
@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShareCapitalController {

    private final ShareCapitalService shareCapitalService;

    /**
     * Purchase shares for a member
     */
    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> purchaseShares(@RequestBody Map<String, Object> request) {
        try {
            UUID memberId = UUID.fromString((String) request.get("memberId"));
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String paymentReference = (String) request.get("paymentReference");

            ShareCapital shareCapital = shareCapitalService.purchaseShares(memberId, amount, paymentReference);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Share purchase successful",
                    "data", Map.of(
                            "totalShares", shareCapital.getTotalShares(),
                            "paidShares", shareCapital.getPaidShares(),
                            "paidAmount", shareCapital.getPaidAmount(),
                            "shareValue", shareCapital.getShareValue()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get member's share capital details
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberShares(@PathVariable UUID memberId) {
        try {
            ShareCapital shareCapital = shareCapitalService.getMemberShares(memberId);
            BigDecimal percentage = shareCapitalService.getMemberSharePercentage(memberId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "totalShares", shareCapital.getTotalShares(),
                            "paidShares", shareCapital.getPaidShares(),
                            "paidAmount", shareCapital.getPaidAmount(),
                            "shareValue", shareCapital.getShareValue(),
                            "ownershipPercentage", percentage
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get total SACCO share capital
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotalShareCapital() {
        BigDecimal total = shareCapitalService.getTotalShareCapital();
        BigDecimal shareValue = shareCapitalService.getShareValue();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "totalShareCapital", total,
                        "shareValue", shareValue,
                        "totalShares", total.divide(shareValue, 2, java.math.RoundingMode.DOWN)
                )
        ));
    }

    /**
     * Get current share value
     */
    @GetMapping("/value")
    public ResponseEntity<Map<String, Object>> getShareValue() {
        BigDecimal shareValue = shareCapitalService.getShareValue();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "shareValue", shareValue,
                        "currency", "KES"
                )
        ));
    }

    /**
     * Recalculate all share capital records (Admin only)
     * Use this when share value changes or to fix data inconsistencies
     */
    @PostMapping("/admin/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateAllShares() {
        try {
            int updatedCount = shareCapitalService.recalculateAllShares();
            BigDecimal currentShareValue = shareCapitalService.getShareValue();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully recalculated " + updatedCount + " share capital records",
                    "data", Map.of(
                            "recordsUpdated", updatedCount,
                            "shareValue", currentShareValue
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Recalculation failed: " + e.getMessage()
            ));
        }
    }
}

