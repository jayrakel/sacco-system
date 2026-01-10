package com.sacco.sacco_system.modules.payment.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;
import com.sacco.sacco_system.modules.finance.domain.entity.Transaction;
import com.sacco.sacco_system.modules.finance.domain.repository.TransactionRepository;
import com.sacco.sacco_system.modules.finance.domain.service.AccountingService;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final MpesaConfig mpesaConfig;
    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final SystemSettingService systemSettingService;
    private final AccountingService accountingService;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_PHONE_NUMBER = "254000000000";

    // ✅ UPDATED: Now accepts draftId (referenceId)
    @Transactional
    public Map<String, Object> initiateLoanFeePayment(String email, String phoneNumber, String draftId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 1. IDEMPOTENCY CHECK: Check if payment already exists for this Draft
        List<PaymentLog> existingLogs = paymentLogRepository.findByReferenceIdAndTransactionTypeOrderByUpdatedAtDesc(
                draftId, "LOAN_APPLICATION_FEE"
        );

        if (!existingLogs.isEmpty()) {
            PaymentLog latest = existingLogs.get(0);

            // Scenario A: Already Paid -> Return Success immediately
            if (latest.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
                return Map.of(
                        "success", true,
                        "checkoutRequestId", latest.getCheckoutRequestId(),
                        "message", "Payment already completed",
                        "status", "COMPLETED"
                );
            }

            // Scenario B: Pending -> Reuse existing request (Don't charge again)
            // Timeout logic: If pending for > 5 minutes, consider it stale and allow new one
            boolean isRecent = latest.getUpdatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(5));
            if (latest.getStatus() == PaymentLog.PaymentStatus.PENDING && isRecent) {
                log.info("Reuse pending payment request {} for draft {}", latest.getCheckoutRequestId(), draftId);
                return Map.of(
                        "success", true,
                        "checkoutRequestId", latest.getCheckoutRequestId(),
                        "message", "Existing request found. Please check your phone."
                );
            }
        }

        // --- NEW PAYMENT INITIATION ---

        String feeStr = systemSettingService.getString("LOAN_APPLICATION_FEE", "500");
        BigDecimal amountBD = new BigDecimal(feeStr);
        int amount = amountBD.intValue();

        String formattedPhone = formatPhoneNumber(phoneNumber);

        // DEV BYPASS
        if (TEST_PHONE_NUMBER.equals(formattedPhone)) {
            return mockTestPayment(member, formattedPhone, amountBD, draftId);
        }

        // REAL SAFARICOM CALL
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
            payload.put("TransactionDesc", "Loan App Fee"); // Shortened description

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.get("application/json"));
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

                    // ✅ LINK TO DRAFT: Save referenceId
                    PaymentLog logEntry = PaymentLog.builder()
                            .member(member)
                            .checkoutRequestId(checkoutReqId)
                            .phoneNumber(formattedPhone)
                            .amount(amountBD)
                            .transactionType("LOAN_APPLICATION_FEE")
                            .referenceId(draftId) // CRITICAL: Link logic
                            .status(PaymentLog.PaymentStatus.PENDING)
                            .resultDescription("STK Push Initiated")
                            .build();
                    paymentLogRepository.save(logEntry);

                    return Map.of("success", true, "checkoutRequestId", checkoutReqId, "message", "Request sent");
                } else {
                    throw new RuntimeException("MPESA Error: " + (json.has("errorMessage") ? json.get("errorMessage").asText() : "Unknown Error"));
                }
            }
        } catch (Exception e) {
            log.error("Payment Init Failed", e);
            throw new RuntimeException("Failed to initiate: " + e.getMessage());
        }
    }

    private Map<String, Object> mockTestPayment(Member member, String phone, BigDecimal amount, String draftId) {
        log.info("🧪 TEST MODE: Skipping Safaricom STK Push");
        String mockReqId = "TEST-" + UUID.randomUUID().toString();

        PaymentLog logEntry = PaymentLog.builder()
                .member(member)
                .checkoutRequestId(mockReqId)
                .phoneNumber(phone)
                .amount(amount)
                .transactionType("LOAN_APPLICATION_FEE")
                .referenceId(draftId) // CRITICAL
                .status(PaymentLog.PaymentStatus.PENDING)
                .resultDescription("Test Transaction Initiated")
                .build();
        paymentLogRepository.save(logEntry);

        return Map.of("success", true, "checkoutRequestId", mockReqId, "message", "[TEST MODE] Success");
    }

    // ... (checkPaymentStatus, updateLogStatus, createLedger, getAccessToken, formatPhoneNumber remain same) ...
    // Ensure checkPaymentStatus uses the createLedgerAndAccountingEntries method I gave you in the previous step

    @Transactional
    public Map<String, Object> checkPaymentStatus(String checkoutRequestId) {
        PaymentLog paymentLog = paymentLogRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payment Session not found"));

        if (checkoutRequestId.startsWith("TEST-")) {
            if (paymentLog.getStatus() != PaymentLog.PaymentStatus.COMPLETED) {
                updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, "Test Payment Success");
                createLedgerAndAccountingEntries(paymentLog);
            }
            return Map.of("status", "COMPLETED", "message", "Test Payment Successful");
        }

        if (paymentLog.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
            return Map.of("status", "COMPLETED", "message", "Payment Successful");
        }

        // ... Real Safaricom Query Logic ...
        // Ensure success block calls createLedgerAndAccountingEntries(paymentLog);

        // COPY-PASTE SAFARICOM QUERY LOGIC FROM PREVIOUS RESPONSE HERE FOR COMPLETENESS
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

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.get("application/json"));
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
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, desc);
                        createLedgerAndAccountingEntries(paymentLog);
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
                        if (desc.toLowerCase().contains("process")) {
                            return Map.of("status", "PENDING", "message", "Processing...");
                        }
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.FAILED, desc);
                        return Map.of("status", "FAILED", "message", desc);
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

    private void createLedgerAndAccountingEntries(PaymentLog log) {
        if (transactionRepository.findByExternalReference(log.getCheckoutRequestId()).isPresent()) {
            return;
        }

        Transaction tx = Transaction.builder()
                .member(log.getMember())
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA)
                .amount(log.getAmount())
                .referenceCode("MPESA-" + log.getCheckoutRequestId().substring(0, 8))
                .externalReference(log.getCheckoutRequestId())
                .description(log.getTransactionType())
                .transactionDate(java.time.LocalDateTime.now())
                .balanceAfter(BigDecimal.ZERO)
                .build();

        transactionRepository.save(tx);

        accountingService.postEvent(
                "LOAN_APPLICATION_FEE",
                "Loan App Fee: " + log.getPhoneNumber(),
                log.getCheckoutRequestId(),
                log.getAmount()
        );
    }

    // ... getAccessToken, formatPhoneNumber ... (keep existing)
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