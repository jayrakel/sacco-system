package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.CashFlow;
import com.sacco.sacco_system.modules.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, UUID> {

    List<CashFlow> findByMemberOrderByTransactionDateDesc(Member member);

    List<CashFlow> findByTypeOrderByTransactionDateDesc(CashFlow.TransactionType type);

    List<CashFlow> findByDirectionOrderByTransactionDateDesc(CashFlow.FlowDirection direction);

    List<CashFlow> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    List<CashFlow> findByRelatedEntityId(UUID entityId);

    // Get total inflows
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CashFlow c WHERE c.direction = 'INFLOW'")
    BigDecimal getTotalInflows();

    // Get total outflows
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CashFlow c WHERE c.direction = 'OUTFLOW'")
    BigDecimal getTotalOutflows();

    // Get net cash flow
    @Query("SELECT COALESCE(SUM(CASE WHEN c.direction = 'INFLOW' THEN c.amount ELSE -c.amount END), 0) FROM CashFlow c")
    BigDecimal getNetCashFlow();

    // Get cash flow for a date range
    @Query("SELECT COALESCE(SUM(CASE WHEN c.direction = 'INFLOW' THEN c.amount ELSE -c.amount END), 0) " +
           "FROM CashFlow c WHERE c.transactionDate BETWEEN :start AND :end")
    BigDecimal getNetCashFlowBetween(LocalDateTime start, LocalDateTime end);
}

