package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Optional<Loan> findByLoanNumber(String loanNumber);
    List<Loan> findByMemberId(UUID memberId);
    List<Loan> findByStatus(Loan.LoanStatus status);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l WHERE l.status = 'DISBURSED'")
    BigDecimal getTotalDisbursedLoans();

    @Query("SELECT SUM(l.loanBalance) FROM Loan l WHERE l.status IN ('DISBURSED', 'DEFAULTED')")
    BigDecimal getTotalOutstandingLoans();

    @Query("SELECT SUM(l.totalInterest) FROM Loan l WHERE l.status != 'REJECTED'")
    BigDecimal getTotalInterest();
}