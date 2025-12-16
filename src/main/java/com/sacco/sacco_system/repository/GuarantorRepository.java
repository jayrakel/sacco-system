package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Guarantor;
import com.sacco.sacco_system.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuarantorRepository extends JpaRepository<Guarantor, UUID> {

    List<Guarantor> findByLoanId(UUID loanId);

    List<Guarantor> findByMemberId(UUID memberId);

    // ✅ ADDED: Required for LoanService workflow checks
    long countByLoanAndStatus(Loan loan, Guarantor.GuarantorStatus status);
}