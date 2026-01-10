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

    // ✅ FIX 3: HTTP Timeouts Configured
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_PHONE_NUMBER = "254000000000";

    /**
     * ✅ FIX 2: Refactored to remove @Transactional from HTTP call boundaries.
     * This prevents DB connection exhaustion during slow Safaricom responses.
     */
    public Map<String, Object> initiateLoanFeePayment(String email, String phoneNumber, String draftId) {
        // STEP 1: DB Read (Fast, Transactional)
        // Checks idempotency and fetches member details
        Optional<Map<String, Object>> existingRequest = checkExistingPayment(draftId);
        if (existingRequest.isPresent()) {
            return existingRequest.get();
        }

        Member member = getMemberByEmail(email); // Helper for DB read
        String feeStr = systemSettingService.getString("LOAN_APPLICATION_FEE", "500");
        BigDecimal amountBD = new BigDecimal(feeStr);
        String formattedPhone = formatPhoneNumber(phoneNumber);

        // DEV BYPASS
        if (TEST_PHONE_NUMBER.equals(formattedPhone)) {
            return mockTestPayment(member, formattedPhone, amountBD, draftId);
        }

        // STEP 2: HTTP Call (Slow, NO Transaction)
        // Safaricom API call happens here without holding a DB connection
        Map<String, Object> stkResult = performStkPush(formattedPhone, amountBD.intValue());

        // STEP 3: DB Write (Fast, Transactional)
        // Save the result of the HTTP call
        if ((boolean) stkResult.get("success")) {
            saveNewPaymentLog(member, formattedPhone, amountBD, draftId, (String) stkResult.get("checkoutRequestId"));
        }

        return stkResult;
    }

    // --- Helper: Read-Only Transaction ---
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> checkExistingPayment(String draftId) {
        List<PaymentLog> existingLogs = paymentLogRepository.findByReferenceIdAndTransactionTypeOrderByUpdatedAtDesc(
                draftId, "LOAN_APPLICATION_FEE"
        );

        if (!existingLogs.isEmpty()) {
            PaymentLog latest = existingLogs.get(0);
            if (latest.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
                return Optional.of(Map.of(
                        "success", true,
                        "checkoutRequestId", latest.getCheckoutRequestId(),
                        "message", "Payment already completed",
                        "status", "COMPLETED"
                ));
            }
            boolean isRecent = latest.getUpdatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(5));
            if (latest.getStatus() == PaymentLog.PaymentStatus.PENDING && isRecent) {
                return Optional.of(Map.of(
                        "success", true,
                        "checkoutRequestId", latest.getCheckoutRequestId(),
                        "message", "Existing request found."
                ));
            }
        }
        return Optional.empty();
    }

    // --- Helper: Read Transaction ---
    @Transactional(readOnly = true)
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    // --- Helper: Write Transaction ---
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

    // --- Core Logic: The HTTP Call (No DB Code here) ---
    private Map<String, Object> performStkPush(String phone, int amount) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) throw new RuntimeException("MPESA Auth Failed");

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
                } else {
                    throw new RuntimeException("MPESA Error: " + (json.has("errorMessage") ? json.get("errorMessage").asText() : "Unknown Error"));
                }
            }
        } catch (Exception e) {
            log.error("Payment Init Failed", e);
            throw new RuntimeException("Failed to initiate: " + e.getMessage());
        }
    }

    // --- Callback Processing (Transactional) ---
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

            if (paymentLog == null) {
                log.warn("⚠️ No log found for CheckoutRequestID: {}", checkoutRequestId);
                return;
            }

            if (paymentLog.getStatus() == PaymentLog.PaymentStatus.COMPLETED) {
                log.info("✅ Payment {} already completed.", checkoutRequestId);
                return;
            }

            if (resultCode == 0) {
                updateLogStatus(paymentLog, PaymentLog.PaymentStatus.COMPLETED, resultDesc);
                createLedgerAndAccountingEntries(paymentLog);
                log.info("✅ Payment Confirmed via Callback: {}", checkoutRequestId);
            } else {
                PaymentLog.PaymentStatus status = (resultCode == 1032) ? PaymentLog.PaymentStatus.CANCELLED : PaymentLog.PaymentStatus.FAILED;
                updateLogStatus(paymentLog, status, resultDesc);
                log.error("❌ Payment Failed (Code {}): {}", resultCode, resultDesc);
            }

        } catch (Exception e) {
            log.error("❌ Error parsing callback payload", e);
        }
    }

    // ... (Test Mode, Check Status, Ledger, Helpers remain unchanged from previous correct version)

    private Map<String, Object> mockTestPayment(Member member, String phone, BigDecimal amount, String draftId) {
        String mockReqId = "TEST-" + UUID.randomUUID().toString();
        // Use local Transactional method to save
        saveNewPaymentLog(member, phone, amount, draftId, mockReqId);
        return Map.of("success", true, "checkoutRequestId", mockReqId, "message", "[TEST MODE] Success");
    }

    @Transactional
    public Map<String, Object> checkPaymentStatus(String checkoutRequestId) {
        // [Existing checkPaymentStatus logic]
        // Ensure you rely on createLedgerAndAccountingEntries inside it
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
                        createLedgerAndAccountingEntries(paymentLog);
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

    private void createLedgerAndAccountingEntries(PaymentLog paymentLog) {
        if (transactionRepository.findByExternalReference(paymentLog.getCheckoutRequestId()).isPresent()) {
            return;
        }

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