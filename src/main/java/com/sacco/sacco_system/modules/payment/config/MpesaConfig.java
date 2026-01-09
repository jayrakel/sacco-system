package com.sacco.sacco_system.modules.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mpesa")
public class MpesaConfig {
    // These should be set in application.properties
    private String consumerKey;
    private String consumerSecret;
    private String passKey;
    private String shortCode;
    private String transactionType; // CustomerPayBillOnline
    private String callbackUrl;

    // Sandbox URLs
    private String authUrl = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    private String stkPushUrl = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
    private String queryUrl = "https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query";
}