package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.Guarantor;
import com.sacco.sacco_system.modules.loan.domain.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {

    /**
     * Checks if a member is guaranteeing any loans that have specific statuses (e.g., ACTIVE, IN_ARREARS).
     * Used for eligibility checks to prevent members with risky guarantees from borrowing.
     */
    boolean existsByMemberIdAndLoanStatusIn(UUID memberId, Collection<Loan.LoanStatus> statuses);
}