package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {

    /**
     * Finds the single currently active fiscal period.
     * Returns Optional so we can handle cases where no period is open.
     */
    Optional<FiscalPeriod> findByActiveTrue();

    /**
     * Checks if ANY fiscal period is currently active.
     * Useful for initialization checks.
     */
    @Query("SELECT COUNT(f) > 0 FROM FiscalPeriod f WHERE f.active = true")
    boolean hasActivePeriod();

    /**
     * Find a period by exact dates (Good for preventing duplicates)
     */
    Optional<FiscalPeriod> findByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);
}