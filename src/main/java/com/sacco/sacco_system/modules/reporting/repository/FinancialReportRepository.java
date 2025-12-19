package com.sacco.sacco_system.modules.reporting.repository;

import com.sacco.sacco_system.modules.reporting.model.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, java.util.UUID> {

    Optional<FinancialReport> findByReportDate(LocalDate reportDate);

    // Used for the default "Last 7 days" view if needed
    List<FinancialReport> findTop7ByOrderByReportDateDesc();

    // ✅ NEW: Flexible date range fetching (Sorted Chronologically for the Graph)
    // This supports the 7, 30, 90 day filters
    List<FinancialReport> findByReportDateBetweenOrderByReportDateAsc(LocalDate startDate, LocalDate endDate);
}