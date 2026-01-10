package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
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
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {

    /**
     * Checks if a member is guaranteeing any loans that have specific statuses.
     */
    boolean existsByMemberIdAndLoanLoanStatusIn(UUID memberId, Collection<Loan.LoanStatus> statuses);

    /**
     * Needed for "My Guarantor Requests" dashboard widget
     */
    List<Guarantor> findByMemberAndStatus(Member member, Guarantor.GuarantorStatus status);

    /**
     * Find all guarantors for a specific loan
     */
    List<Guarantor> findAllByLoan(Loan loan);

    /**
     * Check if a specific member is already a guarantor for a specific loan
     */
    boolean existsByLoanAndMember(Loan loan, Member member);

    /**
     * ✅ NEW: Financial Guardrail Query
     * Calculates the Total Amount this member has currently pledged as 'ACCEPTED'
     * for loans that are still 'ACTIVE' or 'IN_ARREARS'.
     * Used to calculate Free Margin (Deposits - Own Loans - Active Guarantees).
     */
    @Query("SELECT COALESCE(SUM(g.guaranteedAmount), 0) FROM Guarantor g " +
            "WHERE g.member.id = :memberId " +
            "AND g.status = 'ACCEPTED' " +
            "AND g.loan.loanStatus IN ('ACTIVE', 'IN_ARREARS')")
    BigDecimal getTotalActiveLiability(@Param("memberId") UUID memberId);

    // Optional: If you ever need to fetch the specific record by IDs
    Optional<Guarantor> findByLoanIdAndMemberId(UUID loanId, UUID memberId);
}