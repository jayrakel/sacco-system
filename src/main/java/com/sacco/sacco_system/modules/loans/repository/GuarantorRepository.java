package com.sacco.sacco_system.modules.loans.repository;

import com.sacco.sacco_system.modules.loans.model.Guarantor;
import com.sacco.sacco_system.modules.loans.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {
    List<Guarantor> findByLoan(Loan loan);
    long countByLoanAndStatus(Loan loan, Guarantor.GuarantorStatus status);

    // ✅ NEW: Find requests sent TO this member
    List<Guarantor> findByMemberIdAndStatus(UUID memberId, Guarantor.GuarantorStatus status);
}