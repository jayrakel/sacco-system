package com.sacco.sacco_system.modules.loan.domain.service;

import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.member.domain.entity.EmploymentDetails;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount; // ✅ Added
import com.sacco.sacco_system.modules.savings.domain.repository.SavingsAccountRepository; // ✅ Added
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LoanLimitService {

    private final LoanRepository loanRepository;
    private final SystemSettingService systemSettingService;
    private final SavingsAccountRepository savingsAccountRepository; // ✅ Injected

    /**
     * Calculate available loan limit for a member
     * Returns the LOWER of Savings-Based Limit and Ability-to-Pay Limit.
     */
    public BigDecimal calculateMemberLoanLimit(Member member) {
        Map<String, Object> details = calculateMemberLoanLimitWithDetails(member);
        return (BigDecimal) details.get("availableLimit");
    }

    /**
     * Calculate loan limit with full breakdown - for loan officer review
     */
    public Map<String, Object> calculateMemberLoanLimitWithDetails(Member member) {
        Map<String, Object> result = new HashMap<>();

        // =================================================================================
        // 1. SAVINGS BASED LIMIT (The "Collateral" Limit)
        // =================================================================================
        double multiplier = systemSettingService.getDouble("LOAN_LIMIT_MULTIPLIER");
        if (multiplier <= 0) multiplier = 3.0;

        // ✅ FIX: Calculate Total Savings from actual Accounts, not the cached Member field
        BigDecimal savings = savingsAccountRepository.findByMember_Id(member.getId())
                .stream()
                .map(SavingsAccount::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal savingsBasedLimit = savings.multiply(BigDecimal.valueOf(multiplier));

        // =================================================================================
        // 2. SALARY BASED LIMIT (The "Ability to Pay" Limit - 1/3rd Rule)
        // =================================================================================
        BigDecimal salaryBasedLimit = BigDecimal.ZERO;
        BigDecimal netSalary = BigDecimal.ZERO;
        boolean hasIncomeData = false;

        EmploymentDetails emp = member.getEmploymentDetails();
        if (emp != null && emp.getNetMonthlyIncome() != null && emp.getNetMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            netSalary = emp.getNetMonthlyIncome();
            hasIncomeData = true;

            // Rule: Max Monthly Deduction = 2/3 of Net Salary (Member keeps 1/3)
            double maxDebtRatio = systemSettingService.getDouble("MAX_DEBT_RATIO", 0.66); // Default 66%
            BigDecimal maxMonthlyRepayment = netSalary.multiply(BigDecimal.valueOf(maxDebtRatio));

            // Estimate Principal: Assuming a standard max tenure (e.g. 48 months)
            int estimateTenure = 48;
            salaryBasedLimit = maxMonthlyRepayment.multiply(BigDecimal.valueOf(estimateTenure));
        }

        // =================================================================================
        // 3. DETERMINE GROSS LIMIT (Lower of the two)
        // =================================================================================
        // If no income data, fallback to Savings Limit
        BigDecimal grossLimit = hasIncomeData
                ? savingsBasedLimit.min(salaryBasedLimit)
                : savingsBasedLimit;

        // =================================================================================
        // 4. SUBTRACT EXISTING DEBT (Strict Calculation)
        // =================================================================================
        List<Loan> allLoans = loanRepository.findByMemberId(member.getId());

        // A: Currently Owing
        BigDecimal currentDebt = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED || l.getStatus() == Loan.LoanStatus.ACTIVE)
                .map(l -> l.getLoanBalance() != null ? l.getLoanBalance() : l.getPrincipalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // B: Pending / Approved but not Disbursed
        BigDecimal pendingDebt = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.ADMIN_APPROVED ||
                        l.getStatus() == Loan.LoanStatus.TREASURER_DISBURSEMENT ||
                        l.getStatus() == Loan.LoanStatus.SECRETARY_DECISION ||
                        l.getStatus() == Loan.LoanStatus.VOTING_CLOSED ||
                        l.getStatus() == Loan.LoanStatus.VOTING_OPEN ||
                        l.getStatus() == Loan.LoanStatus.ON_AGENDA ||
                        l.getStatus() == Loan.LoanStatus.SECRETARY_TABLED ||
                        l.getStatus() == Loan.LoanStatus.LOAN_OFFICER_REVIEW ||
                        l.getStatus() == Loan.LoanStatus.SUBMITTED ||
                        l.getStatus() == Loan.LoanStatus.GUARANTORS_PENDING ||
                        l.getStatus() == Loan.LoanStatus.APPLICATION_FEE_PENDING)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommitted = currentDebt.add(pendingDebt);
        BigDecimal availableLimit = grossLimit.subtract(totalCommitted);

        // =================================================================================
        // 5. CHECKS & BALANCES
        // =================================================================================
        boolean hasDefaults = allLoans.stream()
                .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED || l.getStatus() == Loan.LoanStatus.WRITTEN_OFF);

        if (hasDefaults) {
            availableLimit = BigDecimal.ZERO;
        }
        if (availableLimit.compareTo(BigDecimal.ZERO) < 0) {
            availableLimit = BigDecimal.ZERO;
        }

        // Populate Result for UI
        result.put("memberSavings", savings);
        result.put("multiplier", multiplier);
        result.put("netSalary", netSalary);
        result.put("hasIncomeData", hasIncomeData);

        result.put("savingsBasedLimit", savingsBasedLimit);
        result.put("salaryBasedLimit", salaryBasedLimit);
        result.put("grossLimit", grossLimit);

        result.put("currentDebt", currentDebt);
        result.put("pendingDebt", pendingDebt);
        result.put("totalCommitted", totalCommitted);

        result.put("availableLimit", availableLimit);
        result.put("hasDefaults", hasDefaults);
        result.put("canBorrow", !hasDefaults && availableLimit.compareTo(BigDecimal.ZERO) > 0);

        return result;
    }

    public boolean canMemberBorrow(Member member, BigDecimal requestedAmount) {
        BigDecimal availableLimit = calculateMemberLoanLimit(member);
        return availableLimit.compareTo(requestedAmount) >= 0;
    }
}