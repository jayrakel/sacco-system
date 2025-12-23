package com.sacco.sacco_system.modules.finance.domain.service;

import java.math.BigDecimal;

public interface BankService {
    BigDecimal getAccountBalance();
    String getBankName();
}
