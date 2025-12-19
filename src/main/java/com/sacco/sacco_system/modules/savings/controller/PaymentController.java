package com.sacco.sacco_system.modules.savings.controller;

import com.sacco.sacco_system.modules.savings.service.PaymentService;
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

    // Trigger STK Push (Simulated)
    @PostMapping("/mpesa/stk")
    public ResponseEntity<?> triggerStkPush(
            @RequestParam UUID memberId,
            @RequestParam BigDecimal amount,
            @RequestParam String phoneNumber) {

        try {
            String reqId = paymentService.initiateMpesaPayment(memberId, amount, phoneNumber, "Deposit");
            return ResponseEntity.ok(Map.of("success", true, "message", "STK Push Sent", "requestId", reqId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Callback (Simulated - You would call this via Postman to fake a payment)
    @PostMapping("/mpesa/callback")
    public ResponseEntity<?> receiveCallback(@RequestBody Map<String, Object> payload) {
        // In real life, extract fields from Safaricom JSON
        String checkoutRequestId = (String) payload.get("CheckoutRequestID");
        String mpesaCode = (String) payload.get("MpesaReceiptNumber");
        String amountStr = (String) payload.get("Amount");
        String phone = (String) payload.get("PhoneNumber");

        if (mpesaCode != null) {
            paymentService.processPaymentCallback(checkoutRequestId, mpesaCode, new BigDecimal(amountStr), phone);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().build();
    }
}