package com.sacco.sacco_system.modules.finance.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Profile("prod") // Active only in production
public class CoopBankService implements BankService {

    // ✅ Inject variables from application.properties
    @Value("${bank.coop.account-number}")
    private String accountNumber;

    @Value("${bank.coop.consumer-key}")
    private String consumerKey;

    @Value("${bank.coop.consumer-secret}")
    private String consumerSecret;

    @Override
    public BigDecimal getAccountBalance() {
        // TODO: Implement actual Co-op API logic using consumerKey & consumerSecret
        System.out.println("🔌 Connecting to CO-OPERATIVE BANK (Corporate Account: " + accountNumber + ")...");
        
        // Return dummy balance for now until you implement the HTTP call
        return new BigDecimal("1500000.00");
    }

    @Override
    public String getBankName() {
        return "Co-operative Bank";
    }
}