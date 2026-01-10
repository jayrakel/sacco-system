package com.sacco.sacco_system.modules.payment.api.controller;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import com.sacco.sacco_system.modules.payment.domain.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mpesa/pay-loan-fee")
    public ResponseEntity<ApiResponse<Object>> payLoanFee(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {

        String phoneNumber = request.get("phoneNumber");
        String draftId = request.get("draftId"); // ✅ Extract Draft ID

        if (draftId == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Draft ID is required", null));
        }

        Map<String, Object> response = paymentService.initiateLoanFeePayment(
                userDetails.getUsername(),
                phoneNumber,
                draftId
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Processing", response));
    }

    // ... Check Status Endpoint (Unchanged) ...
    @GetMapping("/mpesa/check-status/{checkoutRequestId}")
    public ResponseEntity<ApiResponse<Object>> checkStatus(@PathVariable String checkoutRequestId) {
        Map<String, Object> status = paymentService.checkPaymentStatus(checkoutRequestId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment Status Checked", status));
    }

    @PostMapping("/callback")
    public void handleMpesaCallback(@RequestBody String rawPayload) {
        // Just log it for now to see what Safaricom sends
        System.out.println("📩 M-Pesa Callback Received: " + rawPayload);
    }
}