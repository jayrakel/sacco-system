package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.FinancialReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, UUID> {
    Optional<FinancialReport> findByReportDate(LocalDate reportDate);
}
