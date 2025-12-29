package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan.LoanStatus; // ✅ Import the Enum correctly
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    /**
     * ✅ FIXED: Changed return type from 'KeyValues' to 'List<Loan>'
     * Used by Admin Dashboard to fetch SUBMITTED, APPROVED, SECRETARY_TABLED, etc.
     */
    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    // Used for Eligibility (checking max active loans)
    long countByMemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // Used for Eligibility (checking if specific status exists)
    boolean existsByMemberIdAndStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // Used for "My Loans"
    List<Loan> findByMemberId(UUID memberId);

    // Simple single-status fetch
    List<Loan> findByStatus(LoanStatus status);

    /**
     * ✅ Calculates total volume of money disbursed.
     * Includes ACTIVE, DISBURSED, and IN_ARREARS to get the true historical volume.
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS', 'COMPLETED')")
    BigDecimal getTotalDisbursedLoans();

    /**
     * ✅ Calculates total money currently owed to the Sacco.
     */
    @Query("SELECT COALESCE(SUM(l.loanBalance), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS')")
    BigDecimal getTotalOutstandingLoans();
}