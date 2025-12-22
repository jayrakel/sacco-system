package com.sacco.sacco_system.modules.loan.domain.service;
import com.sacco.sacco_system.modules.admin.domain.service.SystemSettingService;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanLimitService {

    private final LoanRepository loanRepository;
    private final SystemSettingService systemSettingService;

    /**
     * Calculate available loan limit for a member
     * STRICT MODE: Considers all loans including pending approvals and disbursements
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

        // 1. Get Settings (Default Multiplier is usually 3x)
        double multiplier = systemSettingService.getDouble("LOAN_LIMIT_MULTIPLIER");
        if (multiplier <= 0) multiplier = 3.0;

        // 2. Base Limit = Total Savings * Multiplier
        BigDecimal savings = member.getTotalSavings() != null ? member.getTotalSavings() : BigDecimal.ZERO;
        BigDecimal baseLimit = savings.multiply(BigDecimal.valueOf(multiplier));

        // 3. Get All Loans for this member
        List<Loan> allLoans = loanRepository.findByMemberId(member.getId());

        // 4. STRICT CALCULATION: Include multiple categories

        // Category A: Currently Owing (DISBURSED, ACTIVE)
        BigDecimal currentDebt = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.DISBURSED ||
                            l.getStatus() == Loan.LoanStatus.ACTIVE)
                .map(l -> l.getLoanBalance() != null ? l.getLoanBalance() : l.getPrincipalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category B: PENDING DISBURSEMENT (Approved but not yet disbursed)
        // These should COUNT against limit because they're already approved!
        BigDecimal pendingDisbursement = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.TREASURER_DISBURSEMENT ||
                            l.getStatus() == Loan.LoanStatus.ADMIN_APPROVED ||
                            l.getStatus() == Loan.LoanStatus.SECRETARY_DECISION ||
                            l.getStatus() == Loan.LoanStatus.VOTING_CLOSED ||
                            l.getStatus() == Loan.LoanStatus.APPROVED) // Legacy status
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category C: UNDER REVIEW (Should also count to prevent double applications)
        BigDecimal underReview = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.SUBMITTED ||
                            l.getStatus() == Loan.LoanStatus.LOAN_OFFICER_REVIEW ||
                            l.getStatus() == Loan.LoanStatus.SECRETARY_TABLED ||
                            l.getStatus() == Loan.LoanStatus.ON_AGENDA ||
                            l.getStatus() == Loan.LoanStatus.VOTING_OPEN)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Category D: PENDING (Waiting for guarantors or fee payment)
        BigDecimal pendingApplication = allLoans.stream()
                .filter(l -> l.getStatus() == Loan.LoanStatus.GUARANTORS_PENDING ||
                            l.getStatus() == Loan.LoanStatus.APPLICATION_FEE_PENDING)
                .map(Loan::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total Committed Amount
        BigDecimal totalCommitted = currentDebt
                .add(pendingDisbursement)
                .add(underReview)
                .add(pendingApplication);

        // Available Limit
        BigDecimal availableLimit = baseLimit.subtract(totalCommitted);

        // 5. Check for Defaults - BLOCK completely if defaults exist
        boolean hasDefaults = allLoans.stream()
                .anyMatch(l -> l.getStatus() == Loan.LoanStatus.DEFAULTED ||
                             l.getStatus() == Loan.LoanStatus.WRITTEN_OFF);

        if (hasDefaults) {
            availableLimit = BigDecimal.ZERO; // Cannot borrow if you have a bad record
        }

        // 6. Ensure non-negative
        if (availableLimit.compareTo(BigDecimal.ZERO) < 0) {
            availableLimit = BigDecimal.ZERO;
        }

        // Build detailed response
        result.put("memberSavings", savings);
        result.put("multiplier", multiplier);
        result.put("baseLimit", baseLimit);
        result.put("currentDebt", currentDebt);
        result.put("pendingDisbursement", pendingDisbursement);
        result.put("underReview", underReview);
        result.put("pendingApplication", pendingApplication);
        result.put("totalCommitted", totalCommitted);
        result.put("availableLimit", availableLimit);
        result.put("hasDefaults", hasDefaults);
        result.put("canBorrow", !hasDefaults && availableLimit.compareTo(BigDecimal.ZERO) > 0);

        return result;
    }

    /**
     * Quick check if member can borrow a specific amount
     */
    public boolean canMemberBorrow(Member member, BigDecimal requestedAmount) {
        BigDecimal availableLimit = calculateMemberLoanLimit(member);
        return availableLimit.compareTo(requestedAmount) >= 0;
    }
}



