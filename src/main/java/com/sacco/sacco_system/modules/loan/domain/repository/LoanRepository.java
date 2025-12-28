package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
    long countByMemberIdAndStatusIn(UUID memberId, List<Loan.LoanStatus> statuses);

    List<Loan> findByMemberId(UUID memberId);

    List<Loan> findByStatus(Loan.LoanStatus status);

    boolean existsByMemberIdAndStatusIn(UUID memberId, Collection<Loan.LoanStatus> statuses);

    /**
     * ✅ Added to resolve FinancialReportService errors
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status = 'ACTIVE' OR l.status = 'DISBURSED'")
    BigDecimal getTotalDisbursedLoans();

    /**
     * ✅ Added to resolve FinancialReportService errors
     */
    @Query("SELECT COALESCE(SUM(l.loanBalance), 0) FROM Loan l WHERE l.status = 'ACTIVE' OR l.status = 'IN_ARREARS'")
    BigDecimal getTotalOutstandingLoans();
}