package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    /**
     * Count transactions for an account within a date range
     */
    @Query("SELECT COUNT(jl) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.accountCode = :accountCode " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    Long countByAccountCodeAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum debit amounts for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(jl.amount), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.accountCode = :accountCode " +
           "AND jl.entryType = 'DEBIT' " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDebitsByAccountAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum credit amounts for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(jl.amount), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.accountCode = :accountCode " +
           "AND jl.entryType = 'CREDIT' " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCreditsByAccountAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
