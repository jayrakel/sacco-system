package com.sacco.sacco_system.modules.finance.domain.service;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service to fix/migrate existing disbursed loans that are missing calculated fields
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanMigrationService {

    private final LoanRepository loanRepository;

    /**
     * Fix all existing disbursed loans that have missing calculated fields
     * This should be run ONCE after deploying the disbursement fix
     */
    @Transactional
    public int fixExistingDisbursedLoans() {
        log.info("🔧 Starting migration to fix existing disbursed loans...");

        // Find all DISBURSED or ACTIVE loans with missing calculations
        List<Loan> loansToFix = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        int fixedCount = 0;

        for (Loan loan : loansToFix) {
            // Check if loan needs fixing
            boolean needsFix = loan.getOutstandingPrincipal() == null
                    || loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0
                    || loan.getOutstandingInterest() == null
                    || loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0
                    || loan.getTotalOutstandingAmount() == null
                    || loan.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0
                    || loan.getWeeklyRepaymentAmount() == null
                    || loan.getMaturityDate() == null;

            if (!needsFix) {
                continue; // Skip loans that are already correct
            }

            try {
                // Get loan parameters
                BigDecimal principal = loan.getDisbursedAmount() != null && loan.getDisbursedAmount().compareTo(BigDecimal.ZERO) > 0
                        ? loan.getDisbursedAmount()
                        : loan.getApprovedAmount();

                BigDecimal interestRate = loan.getInterestRate();
                Integer durationWeeks = loan.getDurationWeeks();

                // Calculate total interest (FLAT rate)
                BigDecimal totalInterest = principal
                        .multiply(interestRate)
                        .multiply(BigDecimal.valueOf(durationWeeks))
                        .divide(BigDecimal.valueOf(5200), 2, BigDecimal.ROUND_HALF_UP);

                BigDecimal totalRepayable = principal.add(totalInterest);

                // Calculate weekly repayment
                BigDecimal weeklyRepayment = totalRepayable
                        .divide(BigDecimal.valueOf(durationWeeks), 2, BigDecimal.ROUND_HALF_UP);

                // Calculate maturity date
                LocalDate disbursementDate = loan.getDisbursementDate() != null
                        ? loan.getDisbursementDate()
                        : LocalDate.now();
                LocalDate maturityDate = disbursementDate.plusWeeks(durationWeeks);

                // Update loan fields
                loan.setOutstandingPrincipal(principal);
                loan.setOutstandingInterest(totalInterest);
                loan.setTotalOutstandingAmount(totalRepayable);
                loan.setWeeklyRepaymentAmount(weeklyRepayment);
                loan.setMaturityDate(maturityDate);

                // Set audit fields if missing
                if (loan.getCreatedBy() == null) {
                    loan.setCreatedBy("SYSTEM_MIGRATION");
                }
                loan.setUpdatedBy("SYSTEM_MIGRATION");

                loanRepository.save(loan);
                fixedCount++;

                log.info("✅ Fixed loan {}: Principal={}, Interest={}, Total={}, Weekly={}, Maturity={}",
                        loan.getLoanNumber(),
                        principal,
                        totalInterest,
                        totalRepayable,
                        weeklyRepayment,
                        maturityDate);

            } catch (Exception e) {
                log.error("❌ Failed to fix loan {}: {}", loan.getLoanNumber(), e.getMessage());
            }
        }

        log.info("🎉 Migration complete! Fixed {} out of {} loans", fixedCount, loansToFix.size());
        return fixedCount;
    }

    /**
     * Get count of loans that need fixing
     */
    public long countLoansNeedingFix() {
        List<Loan> loans = loanRepository.findByLoanStatusIn(
                List.of(Loan.LoanStatus.DISBURSED, Loan.LoanStatus.ACTIVE)
        );

        return loans.stream()
                .filter(loan ->
                        loan.getOutstandingPrincipal() == null
                                || loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) == 0
                                || loan.getOutstandingInterest() == null
                                || loan.getOutstandingInterest().compareTo(BigDecimal.ZERO) == 0
                                || loan.getTotalOutstandingAmount() == null
                                || loan.getTotalOutstandingAmount().compareTo(BigDecimal.ZERO) == 0
                                || loan.getWeeklyRepaymentAmount() == null
                                || loan.getMaturityDate() == null
                )
                .count();
    }
}

