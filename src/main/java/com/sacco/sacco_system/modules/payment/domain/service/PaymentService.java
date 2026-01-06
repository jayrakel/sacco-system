package com.sacco.sacco_system.modules.payment.domain.service;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final MemberRepository memberRepository;
    private final SavingsService savingsService;

    /**
     * INITIATE STK PUSH
     * Triggers the M-Pesa prompt on the user's phone.
     * * @param accountReference What the user sees on their phone (e.g., "Loan Fee")
     * @param transactionDesc Internal description
     */
    public String initiateMpesaPayment(UUID memberId, BigDecimal amount, String phoneNumber, String accountReference, String transactionDesc) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. Validate & Format Phone (Kenyan Format)
        String formattedPhone = formatPhoneNumber(phoneNumber);

        log.info(">>> 📡 INITIATING STK PUSH: {} | Amount: {} | Ref: {}", formattedPhone, amount, accountReference);

        // TODO: INTEGRATE DARAJA API HERE
        // In a real live system, you would use OkHttp or RestTemplate to call:
        // https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest

        // For now, we simulate a successful request ID from Safaricom
        String checkoutRequestId = "ws_CO_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4);

        return checkoutRequestId;
    }

    /**
     * PROCESS CALLBACK (WebHook from Safaricom)
     * This runs when the user enters their PIN.
     */
    public void processPaymentCallback(String checkoutRequestId, String mpesaCode, BigDecimal amount, String phoneNumber) {
        log.info("<<< 📨 PAYMENT SUCCESS: {} | KES {}", mpesaCode, amount);

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("No member found with phone: " + phoneNumber));

        if (member.getSavingsAccounts().isEmpty()) {
            throw new RuntimeException("Member has no savings account to receive funds");
        }

        // 2. Deposit Funds to Member's Account
        // We deposit this as a "General Deposit" so the funds are available
        // for the LoanApplicationService to 'deduct' immediately after.
        savingsService.deposit(
                member.getSavingsAccounts().get(0).getAccountNumber(), // Default to main savings
                amount,
                "M-Pesa: " + mpesaCode + " (Ref: " + checkoutRequestId + ")"
        );

        // Optional: You could save this to a "PaymentLog" table to track status by CheckoutRequestID
    }

    /**
     * Helper to ensure phone number is in 2547... format
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null) return null;
        // Remove spaces or dashes
        String clean = phone.replaceAll("\\s+", "").replaceAll("-", "");

        // Normalize to 2547...
        if (clean.startsWith("07") || clean.startsWith("01")) {
            return "254" + clean.substring(1);
        }
        if (clean.startsWith("+254")) {
            return clean.substring(1);
        }
        return clean;
    }
}