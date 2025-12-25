package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {

    List<LoanRepayment> findByLoanIdOrderByDueDateAsc(UUID loanId);

    Optional<LoanRepayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(UUID loanId, LoanRepayment.RepaymentStatus status);

    /**
     * ✅ UPDATED: Fixed 'totalPaid' to 'amountPaid' to resolve startup crash
     */
    @Query("SELECT SUM(lr.amountPaid) FROM LoanRepayment lr WHERE lr.status = 'PAID'")
    BigDecimal getTotalRepaymentsCollected();

    /**
     * ✅ UPDATED: Fixed 'amount' to 'amountDue' to match LoanRepayment.java
     */
    @Query("SELECT SUM(lr.amountDue) FROM LoanRepayment lr WHERE lr.status = 'PENDING'")
    BigDecimal getTotalExpectedRepayments();
}