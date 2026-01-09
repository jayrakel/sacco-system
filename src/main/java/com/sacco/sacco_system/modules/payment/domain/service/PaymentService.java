package com.sacco.sacco_system.modules.payment.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.member.domain.repository.MemberRepository;
import com.sacco.sacco_system.modules.payment.config.MpesaConfig;
import com.sacco.sacco_system.modules.payment.domain.entity.PaymentLog;
import com.sacco.sacco_system.modules.payment.domain.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final MpesaConfig mpesaConfig;
    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository; // ✅ Connect to Ledger
    private final PaymentLogRepository paymentLogRepository; // ✅ Connect to Audit Log
    private final SystemSettingService systemSettingService;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> initiateLoanFeePayment(String email, String phoneNumber) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Fetch dynamic fee
        String feeStr = systemSettingService.getString("LOAN_APPLICATION_FEE", "500");
        BigDecimal amountBD = new BigDecimal(feeStr);
        int amount = amountBD.intValue();

        String formattedPhone = formatPhoneNumber(phoneNumber);

        try {
            String accessToken = getAccessToken();
            if (accessToken == null) throw new RuntimeException("MPESA Auth Failed");

            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String password = Base64.getEncoder().encodeToString(
                    (mpesaConfig.getShortCode() + mpesaConfig.getPassKey() + timestamp).getBytes()
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", mpesaConfig.getShortCode());
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("TransactionType", "CustomerPayBillOnline");
            payload.put("Amount", amount);
            payload.put("PartyA", formattedPhone);
            payload.put("PartyB", mpesaConfig.getShortCode());
            payload.put("PhoneNumber", formattedPhone);
            payload.put("CallBackURL", mpesaConfig.getCallbackUrl());
            payload.put("AccountReference", "LoanFee");
            payload.put("TransactionDesc", "Loan Application Fee");

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(mpesaConfig.getStkPushUrl())
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode json = objectMapper.readTree(responseBody);

                if (json.has("ResponseCode") && "0".equals(json.get("ResponseCode").asText())) {
                    String checkoutReqId = json.get("CheckoutRequestID").asText();

                    // ✅ AUDIT: Save to PaymentLog (State: PENDING)
                    PaymentLog log = PaymentLog.builder()
                            .member(member)
                            .checkoutRequestId(checkoutReqId)
                            .phoneNumber(formattedPhone)
                            .amount(amountBD)
                            .transactionType("LOAN_APPLICATION_FEE")
                            .status(PaymentLog.PaymentStatus.PENDING)
                            .resultDescription("STK Push Initiated")
                            .build();
                    paymentLogRepository.save(log);

                    return Map.of(
                            "success", true,
                            "checkoutRequestId", checkoutReqId,
                            "message", "Request sent to phone"
                    );
                } else {
                    throw new RuntimeException("MPESA Error: " + (json.has("errorMessage") ? json.get("errorMessage").asText() : "Unknown Error"));
                }
            }
        } catch (Exception e) {
            log.error("Payment Init Failed", e);
            throw new RuntimeException("Failed to initiate: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> checkPaymentStatus(String checkoutRequestId) {
        // 1. Fetch Audit Log
        PaymentLog paymentLog = paymentLogRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payment Session not found"));

        // If finalized, return early (Idempotency)
        if (paymentLog.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
            return Map.of("status", "COMPLETED", "message", "Payment Successful");
        }
        if (paymentLog.getStatus() == PaymentLog.PaymentStatus.FAILED || paymentLog.getStatus() == PaymentLog.PaymentStatus.CANCELLED) {
            return Map.of("status", paymentLog.getStatus().toString(), "message", paymentLog.getResultDescription());
        }

        // 2. Query Safaricom
        try {
            String accessToken = getAccessToken();
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String password = Base64.getEncoder().encodeToString(
                    (mpesaConfig.getShortCode() + mpesaConfig.getPassKey() + timestamp).getBytes()
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", mpesaConfig.getShortCode());
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("CheckoutRequestID", checkoutRequestId);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(mpesaConfig.getQueryUrl())
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode json = objectMapper.readTree(responseBody);

                if (json.has("ResultCode")) {
                    String code = json.get("ResultCode").asText();
                    String desc = json.has("ResultDesc") ? json.get("ResultDesc").asText() : "Unknown";

                    if ("0".equals(code)) {
                        // ✅ SUCCESS: Update Log & Create Ledger Entry
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, desc);
                        createLedgerTransaction(paymentLog);
                        return Map.of("status", "COMPLETED", "message", "Payment Successful");
                    }
                    else if ("1032".equals(code)) {
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.CANCELLED, "User Cancelled");
                        return Map.of("status", "CANCELLED", "message", "You cancelled the payment.");
                    }
                    else if ("1".equals(code)) {
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.FAILED, "Insufficient Funds");
                        return Map.of("status", "FAILED", "message", "Insufficient Funds.");
                    }
                    else {
                        // Handle "Processing" state described in error messages
                        if (desc.toLowerCase().contains("process")) {
                            return Map.of("status", "PENDING", "message", "Processing...");
                        }
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.FAILED, desc);
                        return Map.of("status", "FAILED", "message", desc);
                    }
                }

                if (json.has("errorCode")) {
                    String errorMsg = json.get("errorMessage").asText();
                    if (errorMsg.toLowerCase().contains("process")) {
                        return Map.of("status", "PENDING", "message", "Waiting for M-Pesa...");
                    }
                }

                return Map.of("status", "PENDING", "message", "Waiting...");
            }
        } catch (Exception e) {
            return Map.of("status", "PENDING", "message", "Checking...");
        }
    }

    private void updateLogStatus(PaymentLog log, PaymentLog.PaymentStatus status, String desc) {
        log.setStatus(status);
        log.setResultDescription(desc);
        paymentLogRepository.save(log);
    }

    // ✅ Create Real Financial Record
    private void createLedgerTransaction(PaymentLog log) {
        // Ensure we don't duplicate transactions for the same request
        // (CheckoutRequestId is unique, so you could check if a tx with this extRef exists)

        Transaction tx = Transaction.builder()
                .member(log.getMember())
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA)
                .amount(log.getAmount())
                .referenceCode("MPESA-" + log.getCheckoutRequestId().substring(0, 8))
                .externalReference(log.getCheckoutRequestId())
                .description(log.getTransactionType())
                .balanceAfter(BigDecimal.ZERO)
                .build();

        transactionRepository.save(tx);
    }

    private String getAccessToken() throws IOException {
        String auth = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(mpesaConfig.getAuthUrl())
                .addHeader("Authorization", "Basic " + encodedAuth)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonNode json = objectMapper.readTree(response.body().string());
                return json.get("access_token").asText();
            }
        }
        return null;
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("0")) return "254" + phone.substring(1);
        if (phone.startsWith("+")) return phone.substring(1);
        if (phone.startsWith("7") || phone.startsWith("1")) return "254" + phone;
        return phone;
    }
}