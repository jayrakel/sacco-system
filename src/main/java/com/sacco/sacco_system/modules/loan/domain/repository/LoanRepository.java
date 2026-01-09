package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan.LoanStatus;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    // --- 1. STATUS CHECKS ---

    // Find all loans in the system with specific statuses
    List<Loan> findByLoanStatusIn(Collection<LoanStatus> statuses);

    // Eligibility: Check if member has specific loans (e.g., ACTIVE or IN_ARREARS)
    boolean existsByMemberIdAndLoanStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // Eligibility: Count specific loans
    long countByMemberIdAndLoanStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // --- 2. MEMBER DATA ---

    // Fetch all loans for a specific member by ID
    List<Loan> findByMemberId(UUID memberId);

    // Fetch all loans for a specific member entity
    List<Loan> findByMember(Member member);

    // Simple single-status fetch (Global)
    List<Loan> findByLoanStatus(LoanStatus status);

    // ✅ NEW: Find loans for a member with specific statuses (Used for Draft Resumption)
    // Returns List because a member *might* theoretically have multiple closed drafts (though logic prevents it)
    List<Loan> findByMemberIdAndLoanStatusIn(UUID memberId, Collection<LoanStatus> statuses);

    // ✅ NEW: Find a specific loan by Member and Status (Used for Duplicate Checks)
    Optional<Loan> findByMemberAndLoanStatus(Member member, LoanStatus status);

    // --- 3. DASHBOARD METRICS ---

    /**
     * Calculates total volume of money disbursed historically.
     */
    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.loanStatus IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS', 'CLOSED')")
    BigDecimal getTotalDisbursedLoans();

    /**
     * Calculates total money currently owed (Outstanding Balance).
     */
    @Query("SELECT COALESCE(SUM(l.totalOutstandingAmount), 0) FROM Loan l WHERE l.loanStatus IN ('ACTIVE', 'DISBURSED', 'IN_ARREARS')")
    BigDecimal getTotalOutstandingLoans();

    /**
     * Custom count for Eligibility Service (Alternative to method name query)
     */
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId AND l.loanStatus IN ('ACTIVE', 'IN_ARREARS')")
    long countActiveLoans(@Param("memberId") UUID memberId);
}