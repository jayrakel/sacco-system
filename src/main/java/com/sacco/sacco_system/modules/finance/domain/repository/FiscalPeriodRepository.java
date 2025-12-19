package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {
    FiscalPeriod findByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);
}
