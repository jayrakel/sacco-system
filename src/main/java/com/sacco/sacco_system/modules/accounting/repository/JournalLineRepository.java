package com.sacco.sacco_system.modules.accounting.repository;

import com.sacco.sacco_system.modules.accounting.model.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, java.util.UUID> {

    // Fetch sums grouped by account code up to a specific date
    @Query("SELECT jl.account.code, SUM(jl.debit), SUM(jl.credit) " +
            "FROM JournalLine jl " +
            "JOIN jl.journalEntry je " +
            "WHERE je.transactionDate <= :endDate " +
            "GROUP BY jl.account.code")
    List<Object[]> getAccountTotalsUpToDate(@Param("endDate") LocalDateTime endDate);

    // Fetch sums for a specific date range (For Income Statement)
    @Query("SELECT jl.account.code, SUM(jl.debit), SUM(jl.credit) " +
            "FROM JournalLine jl " +
            "JOIN jl.journalEntry je " +
            "WHERE je.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY jl.account.code")
    List<Object[]> getAccountTotalsInRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}