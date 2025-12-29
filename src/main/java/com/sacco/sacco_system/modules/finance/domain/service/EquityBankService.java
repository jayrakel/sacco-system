package com.sacco.sacco_system.modules.finance.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Profile({"dev", "test"}) // Active only in development or test profiles
public class EquityBankService implements BankService {

    @Value("${bank.equity.account-number}")
    private String accountNumber;

    @Value("${bank.equity.api-key}")
    private String apiKey;

    @Override
    public BigDecimal getAccountBalance() {
        // TODO: Implement actual Jenga API REST call here
        System.out.println("🔌 Connecting to EQUITY BANK (Personal Account: " + accountNumber + ")...");
        
        // For now, return a mock "Live" balance or connect to real API
        // return jengaClient.getBalance(accountNumber);
        return new BigDecimal("54000.00"); // Placeholder
    }

    @Override
    public String getBankName() {
        return "Equity Bank (Test/Personal)";
    }
}