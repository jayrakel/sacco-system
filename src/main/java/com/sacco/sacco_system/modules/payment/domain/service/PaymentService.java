package com.sacco.sacco_system.modules.payment.domain.service;
import com.sacco.sacco_system.modules.savings.domain.service.SavingsService;

import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final SavingsService savingsService;

    /**
     * SIMULATE M-PESA STK PUSH
     * In production, this would call Safaricom Daraja API.
     */
    public String initiateMpesaPayment(UUID memberId, BigDecimal amount, String phoneNumber, String type) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Validate Phone (Basic Regex for Kenya)
        if (!phoneNumber.matches("^(?:254|\\+254|0)?(7(?:(?:[129][0-9])|(?:0[0-8])|(4[0-1]))[0-9]{6})$")) {
            throw new RuntimeException("Invalid Phone Number");
        }

        System.out.println(">>> ðŸ“¡ SENDING STK PUSH TO: " + phoneNumber + " FOR KES " + amount);

        // Mock Response
        return "CheckoutRequestID: ws_CO_" + System.currentTimeMillis();
    }

    /**
     * PROCESS PAYMENT CALLBACK (WebHook)
     * This is called by M-Pesa when the user enters their PIN.
     */
    public void processPaymentCallback(String checkoutRequestId, String mpesaCode, BigDecimal amount, String phoneNumber) {
        System.out.println("<<< ðŸ“¨ PAYMENT RECEIVED: " + mpesaCode + " KES " + amount);

        // Logic to find which Member owns this phone number
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("No member found with phone: " + phoneNumber));

        // Auto-Allocate Funds (Simple Rule: Default to Savings)
        // In a real system, you'd check a 'PendingTransaction' table to know if this was for Loan or Savings
        savingsService.deposit(
                member.getSavingsAccounts().get(0).getAccountNumber(),
                amount,
                "M-Pesa: " + mpesaCode
        );
    }
}

