package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.Loan;
import com.sacco.sacco_system.entity.Member;
import com.sacco.sacco_system.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanLimitService {

    private final LoanRepository loanRepository;
    private final SystemSettingService systemSettingService;

    public BigDecimal calculateMemberLoanLimit(Member member) {
        // 1. Get Settings (Default Multiplier is usually 3x)
        double multiplier = systemSettingService.getDouble("LOAN_LIMIT_MULTIPLIER");
        if (multiplier <= 0) multiplier = 3.0;

        // 2. Base Limit = Total Savings * Multiplier
        BigDecimal savings = member.getTotalSavings();
        BigDecimal baseLimit = savings.multiply(BigDecimal.valueOf(multiplier));

        // 3. Check Active Loans (Reduce limit by amount already owed)
        List<Loan> activeLoans = loanRepository.findByMemberId(member.getId());
        BigDecimal currentDebt = activeLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED ||
                        l.getStatus() == Loan.LoanStatus.ACTIVE ||
                        l.getStatus() == Loan.LoanStatus.APPROVED)
                .map(Loan::getLoanBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalLimit = baseLimit.subtract(currentDebt);

        // 4. (Optional) Check History - Block if Member has any Defaulted loans
        boolean hasDefaults = activeLoans.stream()
                .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED ||
                        l.getStatus() == Loan.LoanStatus.WRITTEN_OFF);

        if (hasDefaults) {
            return BigDecimal.ZERO; // Cannot borrow if you have a bad record
        }

        return finalLimit.compareTo(BigDecimal.ZERO) > 0 ? finalLimit : BigDecimal.ZERO;
    }
}