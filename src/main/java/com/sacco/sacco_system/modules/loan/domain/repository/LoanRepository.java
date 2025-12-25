package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Loan entity operations.
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    /**
     * Find all loans associated with a specific member.
     * Used for "My Loans" and member history checks.
     */
    List<Loan> findByMemberId(UUID memberId);

    /**
     * Find loans by their current workflow status.
     * Useful for fetching loans in VOTING_OPEN or TREASURER_DISBURSEMENT states.
     */
    List<Loan> findByStatus(Loan.LoanStatus status);
}