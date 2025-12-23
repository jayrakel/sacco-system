package com.sacco.sacco_system.modules.finance.domain.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Profile("prod") // Active only in production
public class CoopBankService implements BankService {

    @Override
    public BigDecimal getAccountBalance() {
        // TODO:Implement Co-op Bank API logic here
        System.out.println("🔌 Connecting to CO-OPERATIVE BANK (Corporate)...");
        return new BigDecimal("1500000.00");
    }

    @Override
    public String getBankName() {
        return "Co-operative Bank";
    }
}