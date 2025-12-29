package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {
    List<LoanRepayment> findByLoanIdOrderByDueDateAsc(UUID loanId);
    Optional<LoanRepayment> findFirstByLoanIdAndStatusOrderByDueDateAsc(UUID loanId, LoanRepayment.RepaymentStatus status);

    /** ✅ Fixes FinancialReportService 404/Compilation error */
    @Query("SELECT COALESCE(SUM(lr.amountPaid), 0) FROM LoanRepayment lr WHERE lr.status = 'PAID'")
    BigDecimal getTotalRepaidAmount();

    /** ✅ Fixes FineService compilation error */
    List<LoanRepayment> findByStatusAndDueDateBefore(LoanRepayment.RepaymentStatus status, LocalDate date);
}