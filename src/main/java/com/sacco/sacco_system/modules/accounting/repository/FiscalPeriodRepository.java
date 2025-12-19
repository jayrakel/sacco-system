package com.sacco.sacco_system.modules.accounting.repository;
import com.sacco.sacco_system.modules.accounting.model.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {
    Optional<FiscalPeriod> findByActiveTrue();
}