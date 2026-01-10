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
import org.springframework.transaction.annotation.Propagation; // ✅ Added Import
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TEST_PHONE_NUMBER = "254000000000";

    // ✅ Refactored: No @Transactional on the HTTP method
    public Map<String, Object> initiateLoanFeePayment(String email, String phoneNumber, String draftId) {
        Optional<Map<String, Object>> existing = checkExistingPayment(draftId);
        if (existing.isPresent()) return existing.get();

        Member member = getMemberByEmail(email);
        String feeStr = systemSettingService.getString("LOAN_APPLICATION_FEE", "500");
        BigDecimal amountBD = new BigDecimal(feeStr);
        String formattedPhone = formatPhoneNumber(phoneNumber);

        if (TEST_PHONE_NUMBER.equals(formattedPhone)) {
            return mockTestPayment(member, formattedPhone, amountBD, draftId);
        }

        Map<String, Object> stkResult = performStkPush(formattedPhone, amountBD.intValue());

        if ((boolean) stkResult.get("success")) {
            saveNewPaymentLog(member, formattedPhone, amountBD, draftId, (String) stkResult.get("checkoutRequestId"));
        }
        return stkResult;
    }

    // --- Transactional Helpers ---
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> checkExistingPayment(String draftId) {
        List<PaymentLog> existingLogs = paymentLogRepository.findByReferenceIdAndTransactionTypeOrderByUpdatedAtDesc(
                draftId, "LOAN_APPLICATION_FEE"
        );
        if (!existingLogs.isEmpty()) {
            PaymentLog latest = existingLogs.get(0);
            if (latest.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
                return Optional.of(Map.of("success", true, "checkoutRequestId", latest.getCheckoutRequestId(), "status", "COMPLETED"));
            }
            if (latest.getStatus() == PaymentLog.PaymentStatus.PENDING &&
                    latest.getUpdatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(5))) {
                return Optional.of(Map.of("success", true, "checkoutRequestId", latest.getCheckoutRequestId(), "message", "Request pending"));
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    @Transactional
    public void saveNewPaymentLog(Member member, String phone, BigDecimal amount, String draftId, String reqId) {
        PaymentLog logEntry = PaymentLog.builder()
                .member(member)
                .checkoutRequestId(reqId)
                .phoneNumber(phone)
                .amount(amount)
                .transactionType("LOAN_APPLICATION_FEE")
                .referenceId(draftId)
                .status(PaymentLog.PaymentStatus.PENDING)
                .resultDescription("STK Push Initiated")
                .build();
        paymentLogRepository.save(logEntry);
    }

    private Map<String, Object> performStkPush(String phone, int amount) {
        try {
            String accessToken = getAccessToken();
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String password = Base64.getEncoder().encodeToString((mpesaConfig.getShortCode() + mpesaConfig.getPassKey() + timestamp).getBytes());

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", mpesaConfig.getShortCode());
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("TransactionType", "CustomerPayBillOnline");
            payload.put("Amount", amount);
            payload.put("PartyA", phone);
            payload.put("PartyB", mpesaConfig.getShortCode());
            payload.put("PhoneNumber", phone);
            payload.put("CallBackURL", mpesaConfig.getCallbackUrl());
            payload.put("AccountReference", "LoanFee");
            payload.put("TransactionDesc", "Loan App Fee");

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.get("application/json"));
            Request request = new Request.Builder().url(mpesaConfig.getStkPushUrl()).addHeader("Authorization", "Bearer " + accessToken).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode json = objectMapper.readTree(responseBody);
                if (json.has("ResponseCode") && "0".equals(json.get("ResponseCode").asText())) {
                    return Map.of("success", true, "checkoutRequestId", json.get("CheckoutRequestID").asText(), "message", "Request sent");
                }
                throw new RuntimeException("MPESA Error: " + (json.has("errorMessage") ? json.get("errorMessage").asText() : "Unknown Error"));
            }
        } catch (Exception e) {
            log.error("Payment Init Failed", e);
            throw new RuntimeException("Failed to initiate: " + e.getMessage());
        }
    }

    // ✅ FIX: Callback Processing with Safe Ledger Entry
    @Transactional
    public void processMpesaCallback(String rawPayload) {
        log.info("🔄 Processing M-Pesa Callback...");
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode body = root.path("Body").path("stkCallback");
            String checkoutRequestId = body.path("CheckoutRequestID").asText();
            int resultCode = body.path("ResultCode").asInt();
            String resultDesc = body.path("ResultDesc").asText();

            if (checkoutRequestId.isEmpty()) return;

            PaymentLog paymentLog = paymentLogRepository.findByCheckoutRequestId(checkoutRequestId).orElse(null);
            if (paymentLog == null || paymentLog.getStatus() == PaymentLog.PaymentStatus.COMPLETED) return;

            if (resultCode == 0) {
                // 1. Update Status (Must happen)
                updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, resultDesc);

                // 2. Ledger Entry (Can fail without rolling back status if needed)
                try {
                    createLedgerAndAccountingEntries(paymentLog);
                    log.info("✅ Payment Confirmed via Callback: {}", checkoutRequestId);
                } catch (Exception e) {
                    log.error("⚠️ Payment Confirmed but Ledger Failed for {}. Reason: {}", checkoutRequestId, e.getMessage());
                    // We do NOT re-throw here to avoid rolling back the "COMPLETED" status of the payment itself.
                }
            } else {
                updateLogStatus(paymentLog, resultCode == 1032 ? PaymentLog.PaymentStatus.CANCELLED : PaymentLog.PaymentStatus.FAILED, resultDesc);
                log.error("❌ Payment Failed (Code {}): {}", resultCode, resultDesc);
            }
        } catch (Exception e) {
            log.error("❌ Error parsing callback", e);
        }
    }

    // ... (Test Mode helpers...)
    private Map<String, Object> mockTestPayment(Member member, String phone, BigDecimal amount, String draftId) {
        String mockReqId = "TEST-" + UUID.randomUUID().toString();
        saveNewPaymentLog(member, phone, amount, draftId, mockReqId);
        return Map.of("success", true, "checkoutRequestId", mockReqId, "message", "[TEST MODE] Success");
    }

    // ... (checkPaymentStatus...)
    @Transactional
    public Map<String, Object> checkPaymentStatus(String checkoutRequestId) {
        PaymentLog paymentLog = paymentLogRepository.findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payment Session not found"));

        if (checkoutRequestId.startsWith("TEST-")) {
            if (paymentLog.getStatus() != PaymentLog.PaymentStatus.COMPLETED) {
                updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, "Test Payment Success");
                try { createLedgerAndAccountingEntries(paymentLog); } catch(Exception e) { log.error("Test Ledger Failed", e); }
            }
            return Map.of("status", "COMPLETED", "message", "Test Payment Successful");
        }

        if (paymentLog.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
            return Map.of("status", "COMPLETED", "message", "Payment Successful");
        }
        if (paymentLog.getStatus() != PaymentLog.PaymentStatus.PENDING) {
            return Map.of("status", paymentLog.getStatus().toString(), "message", paymentLog.getResultDescription());
        }
        return querySafaricomStatus(paymentLog);
    }

    private Map<String, Object> querySafaricomStatus(PaymentLog paymentLog) {
        try {
            String accessToken = getAccessToken();
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String password = Base64.getEncoder().encodeToString((mpesaConfig.getShortCode() + mpesaConfig.getPassKey() + timestamp).getBytes());

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", mpesaConfig.getShortCode());
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("CheckoutRequestID", paymentLog.getCheckoutRequestId());

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.get("application/json"));
            Request request = new Request.Builder().url(mpesaConfig.getQueryUrl()).addHeader("Authorization", "Bearer " + accessToken).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode json = objectMapper.readTree(responseBody);

                if (json.has("ResultCode")) {
                    String code = json.get("ResultCode").asText();
                    String desc = json.has("ResultDesc") ? json.get("ResultDesc").asText() : "Unknown";

                    if ("0".equals(code)) {
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, desc);

                        try {
                            createLedgerAndAccountingEntries(paymentLog);
                        } catch(Exception e) {
                            log.error("⚠️ Payment Confirmed but Ledger Failed: {}", e.getMessage());
                        }

                        return Map.of("status", "COMPLETED", "message", "Payment Successful");
                    } else if ("1032".equals(code)) {
                        updateLogStatus(paymentLog, PaymentLog.PaymentStatus.CANCELLED, "User Cancelled");
                        return Map.of("status", "CANCELLED", "message", "Cancelled");
                    } else {
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

    private void updateLogStatus(PaymentLog paymentLog, PaymentLog.PaymentStatus status, String desc) {
        paymentLog.setStatus(status);
        paymentLog.setResultDescription(desc);
        paymentLogRepository.save(paymentLog);
    }

    // ✅ FIX: ISOLATED TRANSACTION for Ledger
    // This prevents "Silent Rollback" error if accounting fails
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createLedgerAndAccountingEntries(PaymentLog paymentLog) {
        if (transactionRepository.findByExternalReference(paymentLog.getCheckoutRequestId()).isPresent()) return;

        Transaction tx = Transaction.builder()
                .member(paymentLog.getMember())
                .type(Transaction.TransactionType.PROCESSING_FEE)
                .paymentMethod(Transaction.PaymentMethod.MPESA)
                .amount(paymentLog.getAmount())
                .referenceCode("MPESA-" + paymentLog.getCheckoutRequestId().substring(0, 8))
                .externalReference(paymentLog.getCheckoutRequestId())
                .description(paymentLog.getTransactionType())
                .transactionDate(java.time.LocalDateTime.now())
                .balanceAfter(BigDecimal.ZERO)
                .build();

        transactionRepository.save(tx);

        accountingService.postEvent(
                "LOAN_APPLICATION_FEE",
                "Loan App Fee: " + paymentLog.getPhoneNumber(),
                paymentLog.getCheckoutRequestId(),
                paymentLog.getAmount()
        );
        log.info("💰 Ledger Updated for ReqID: {}", paymentLog.getCheckoutRequestId());
    }

    private String getAccessToken() throws IOException {
        String auth = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        Request request = new Request.Builder().url(mpesaConfig.getAuthUrl()).addHeader("Authorization", "Basic " + encodedAuth).get().build();
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