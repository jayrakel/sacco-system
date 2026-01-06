package com.sacco.sacco_system.modules.payment.api.controller;

import com.sacco.sacco_system.modules.payment.domain.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ✅ 1. Standard Deposit STK Push
    @PostMapping("/mpesa/stk")
    public ResponseEntity<?> triggerStkPush(
            @RequestParam UUID memberId,
            @RequestParam BigDecimal amount,
            @RequestParam String phoneNumber) {

        try {
            // "Deposit" is the reference shown to the user
            String reqId = paymentService.initiateMpesaPayment(
                    memberId,
                    amount,
                    phoneNumber,
                    "Deposit",
                    "General Savings Deposit"
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "STK Push Sent",
                    "checkoutRequestId", reqId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ 2. NEW: Loan Fee Specific Payment
    // This allows the frontend to specifically request a "Loan Fee" payment
    @PostMapping("/mpesa/pay-fee")
    public ResponseEntity<?> payLoanFee(
            @RequestParam UUID memberId,
            @RequestParam BigDecimal amount,
            @RequestParam String phoneNumber,
            @RequestParam(defaultValue = "Loan Application Fee") String reference) {

        try {
            String reqId = paymentService.initiateMpesaPayment(
                    memberId,
                    amount,
                    phoneNumber,
                    "Loan Fee",  // Short reference for SMS
                    reference    // Full description
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "STK Push Sent. Please enter PIN.",
                    "checkoutRequestId", reqId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ 3. Simulated Callback (WebHook)
    // In production, Safaricom calls this URL
    @PostMapping("/mpesa/callback")
    public ResponseEntity<?> receiveCallback(@RequestBody Map<String, Object> payload) {
        try {
            // Extract fields based on Safaricom's standard JSON format
            // Note: Real Safaricom JSON is nested in Body -> stkCallback -> CallbackMetadata
            // This simulation assumes a simplified flat structure for testing.

            String checkoutRequestId = (String) payload.get("CheckoutRequestID");
            String mpesaCode = (String) payload.get("MpesaReceiptNumber");
            String phone = (String) payload.get("PhoneNumber");

            // Handle Amount safely (could be Integer or String in JSON)
            BigDecimal amount = new BigDecimal(payload.get("Amount").toString());

            if (mpesaCode != null && checkoutRequestId != null) {
                paymentService.processPaymentCallback(checkoutRequestId, mpesaCode, amount, phone);
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Missing M-Pesa Code"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}