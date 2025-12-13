package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {
    List<LoanRepayment> findByLoanId(Long loanId);
    List<LoanRepayment> findByStatus(LoanRepayment.RepaymentStatus status);
    Optional<LoanRepayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(Long loanId, LoanRepayment.RepaymentStatus status);
    
    @Query("SELECT SUM(lr.totalPaid) FROM LoanRepayment lr WHERE lr.status = 'PAID'")
    BigDecimal getTotalRepaidAmount();
    
    @Query("SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.status = 'OVERDUE'")
    long countOverdueRepayments();
}
