package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    /**
     * Count transactions for an account within a date range
     */
    @Query("SELECT COUNT(jl) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account.code = :accountCode " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    Long countByAccountCodeAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum debit amounts for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(jl.debit), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account.code = :accountCode " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDebitsByAccountAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Sum credit amounts for an account within a date range
     */
    @Query("SELECT COALESCE(SUM(jl.credit), 0) FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account.code = :accountCode " +
           "AND je.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCreditsByAccountAndDateRange(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get account totals (debit, credit) up to a specific date
     * Returns: account_code, total_debit, total_credit
     */
    @Query("SELECT jl.account.code, COALESCE(SUM(jl.debit), 0), COALESCE(SUM(jl.credit), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.transactionDate <= :endDate " +
           "GROUP BY jl.account.code")
    List<Object[]> getAccountTotalsUpToDate(@Param("endDate") LocalDateTime endDate);

    /**
     * Get account totals (debit, credit) STRICTLY BEFORE a specific date
     * Used for calculating Opening Balances for a date range
     * Returns: account_code, total_debit, total_credit
     */
    @Query("SELECT jl.account.code, COALESCE(SUM(jl.debit), 0), COALESCE(SUM(jl.credit), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.transactionDate < :startDate " +
           "GROUP BY jl.account.code")
    List<Object[]> getAccountTotalsBeforeDate(@Param("startDate") LocalDate startDate);

    /**
     * Get account totals (debit, credit) within a date range
     * Returns: account_code, total_debit, total_credit
     */
    @Query("SELECT jl.account.code, COALESCE(SUM(jl.debit), 0), COALESCE(SUM(jl.credit), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE je.transactionDate BETWEEN :startDate AND :endDate " +
           "GROUP BY jl.account.code")
    List<Object[]> getAccountTotalsInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}