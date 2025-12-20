package com.sacco.sacco_system.modules.finance.api.controller;

import com.sacco.sacco_system.modules.finance.domain.entity.Dividend;
import com.sacco.sacco_system.modules.finance.domain.service.DividendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dividend Controller
 * Manages dividend declaration and payment to members
 */
@RestController
@RequestMapping("/api/dividends")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DividendController {

    private final DividendService dividendService;

    /**
     * Declare dividends for a fiscal year
     */
    @PostMapping("/declare")
    public ResponseEntity<Map<String, Object>> declareDividends(@RequestBody Map<String, Object> request) {
        try {
            Integer fiscalYear = Integer.parseInt(request.get("fiscalYear").toString());
            BigDecimal totalPool = new BigDecimal(request.get("totalPool").toString());
            String notes = (String) request.get("notes");

            List<Dividend> dividends = dividendService.declareDividends(fiscalYear, totalPool, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dividends declared for " + dividends.size() + " members",
                    "data", dividends
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Pay dividend to a member
     */
    @PostMapping("/pay/{dividendId}")
    public ResponseEntity<Map<String, Object>> payDividend(
            @PathVariable UUID dividendId,
            @RequestBody Map<String, String> request) {
        try {
            String paymentReference = request.get("paymentReference");
            Dividend dividend = dividendService.payDividend(dividendId, paymentReference);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dividend paid successfully",
                    "data", dividend
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Pay all declared dividends for a fiscal year
     */
    @PostMapping("/pay-all/{fiscalYear}")
    public ResponseEntity<Map<String, Object>> payAllDividends(@PathVariable Integer fiscalYear) {
        try {
            List<Dividend> dividends = dividendService.payAllDividends(fiscalYear);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Paid " + dividends.size() + " dividends",
                    "data", dividends
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get member's dividend history
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<Map<String, Object>> getMemberDividends(@PathVariable UUID memberId) {
        List<Dividend> dividends = dividendService.getMemberDividends(memberId);
        BigDecimal totalReceived = dividendService.getTotalDividendsReceived(memberId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "dividends", dividends,
                        "totalReceived", totalReceived
                )
        ));
    }

    /**
     * Get dividends for a fiscal year
     */
    @GetMapping("/year/{fiscalYear}")
    public ResponseEntity<Map<String, Object>> getDividendsByYear(@PathVariable Integer fiscalYear) {
        List<Dividend> dividends = dividendService.getDividendsByYear(fiscalYear);
        Map<String, Object> statistics = dividendService.getDividendStatistics(fiscalYear);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "dividends", dividends,
                        "statistics", statistics
                )
        ));
    }

    /**
     * Get dividend statistics for a year
     */
    @GetMapping("/statistics/{fiscalYear}")
    public ResponseEntity<Map<String, Object>> getDividendStatistics(@PathVariable Integer fiscalYear) {
        Map<String, Object> statistics = dividendService.getDividendStatistics(fiscalYear);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", statistics
        ));
    }

    /**
     * Cancel a dividend (before payment)
     */
    @PostMapping("/cancel/{dividendId}")
    public ResponseEntity<Map<String, Object>> cancelDividend(
            @PathVariable UUID dividendId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.get("reason");
            Dividend dividend = dividendService.cancelDividend(dividendId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Dividend cancelled",
                    "data", dividend
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

