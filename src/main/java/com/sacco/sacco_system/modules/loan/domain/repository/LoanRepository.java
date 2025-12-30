package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    // --- 1. STATUS CHECKS (Fixes "Cannot find symbol") ---

    // Used to find loans with specific statuses (e.g., active or pending)
    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    // Used for Eligibility: Check if member has specific loans (e.g., ACTIVE or IN_ARREARS)
    boolean existsByMemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // Used for Eligibility: Count specific loans
    long countByMemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // --- 2. MEMBER DATA ---

    // Fetch all loans for a specific member
    List<Loan> findByMemberId(UUID memberId);

    // Simple single-status fetch
    List<Loan> findByStatus(LoanStatus status);

    // --- 3. DASHBOARD METRICS ---

    /**
     * Calculates total volume of money disbursed historically.
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS', 'COMPLETED')")
    BigDecimal getTotalDisbursedLoans();

    /**
     * Calculates total money currently owed (Outstanding Balance).
     */
    @Query("SELECT COALESCE(SUM(l.loanBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS')")
    BigDecimal getTotalOutstandingLoans();

    /**
     * Custom count for Eligibility Service (Alternative to method name query)
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId AND l.status IN ('ACTIVE', 'IN_ARREARS')")
    long countActiveLoans(@Param("memberId") UUID memberId);
}