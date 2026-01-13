package com.sacco.sacco_system.modules.loan.domain.repository;

import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepaymentSchedule;
import com.sacco.sacco_system.modules.loan.domain.entity.LoanRepaymentSchedule.InstallmentStatus; // ✅ ADDED Import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // ✅ ADDED Import
import java.util.UUID;

public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, UUID> {

    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    // Used by LoanDailyProcessor to find specific overdue installments
    @Query("SELECT s FROM LoanRepaymentSchedule s WHERE s.loan.id = :loanId AND s.dueDate < :today AND s.status != 'PAID'")
    List<LoanRepaymentSchedule> findOverdueInstallments(@Param("loanId") UUID loanId, @Param("today") LocalDate today);

    // Used to calculate the specific Arrears Amount (Expected - Paid) for past due items
    @Query("SELECT COALESCE(SUM(s.totalDue - s.paidAmount), 0) FROM LoanRepaymentSchedule s WHERE s.loan.id = :loanId AND s.dueDate < :today")
    BigDecimal calculateTotalArrears(@Param("loanId") UUID loanId, @Param("today") LocalDate today);

    // ✅ ADDED: Missing method to fetch next installment
    Optional<LoanRepaymentSchedule> findTopByLoanIdAndStatusInOrderByDueDateAsc(UUID loanId, List<InstallmentStatus> statuses);
}