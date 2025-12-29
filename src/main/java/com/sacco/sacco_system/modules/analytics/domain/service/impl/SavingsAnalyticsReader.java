package com.sacco.sacco_system.modules.analytics.domain.service.impl;

import com.sacco.sacco_system.modules.finance.domain.repository.ShareCapitalRepository;
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavingsAnalyticsReader {

    private final SavingsAccountRepository savingsAccountRepository;
    private final ShareCapitalRepository shareCapitalRepository;

    public Map<String, Object> getSavingsAnalytics() {
        BigDecimal totalSavings = savingsAccountRepository.getTotalActiveAccountsBalance();
        BigDecimal totalShareCapital = shareCapitalRepository.getTotalShareCapital();

        long totalAccounts = savingsAccountRepository.count();
        long activeAccounts = savingsAccountRepository.findAll().stream()
                .filter(a -> a.getStatus() == com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount.AccountStatus.ACTIVE)
                .count();

        List<BigDecimal> balances = savingsAccountRepository.findAll().stream()
                .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal averageSavings = balances.isEmpty() ? BigDecimal.ZERO :
                balances.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(balances.size()), 2, RoundingMode.HALF_UP);

        BigDecimal medianSavings = balances.isEmpty() ? BigDecimal.ZERO : balances.get(balances.size() / 2);

        return Map.of(
                "totalSavings", totalSavings != null ? totalSavings : BigDecimal.ZERO,
                "totalShareCapital", totalShareCapital != null ? totalShareCapital : BigDecimal.ZERO,
                "totalAccounts", totalAccounts,
                "activeAccounts", activeAccounts,
                "averageSavings", averageSavings,
                "medianSavings", medianSavings,
                "totalCapital", (totalSavings != null ? totalSavings : BigDecimal.ZERO)
                        .add(totalShareCapital != null ? totalShareCapital : BigDecimal.ZERO)
        );
    }
}