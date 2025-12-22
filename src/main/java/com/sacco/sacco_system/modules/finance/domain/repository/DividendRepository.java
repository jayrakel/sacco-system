package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.Dividend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface DividendRepository extends JpaRepository<Dividend, UUID> {

    List<Dividend> findByMemberId(UUID memberId);

    List<Dividend> findByFiscalYear(Integer fiscalYear);

    List<Dividend> findByFiscalYearAndStatus(Integer fiscalYear, Dividend.DividendStatus status);

    @Query("SELECT SUM(d.dividendAmount) FROM Dividend d WHERE d.fiscalYear = :year AND d.status = 'PAID'")
    BigDecimal getTotalPaidDividendsByYear(@Param("year") Integer year);

    @Query("SELECT SUM(d.dividendAmount) FROM Dividend d WHERE d.member.id = :memberId AND d.status = 'PAID'")
    BigDecimal getTotalDividendsReceivedByMember(@Param("memberId") UUID memberId);
}

