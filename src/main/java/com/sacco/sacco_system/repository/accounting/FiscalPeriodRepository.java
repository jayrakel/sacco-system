package com.sacco.sacco_system.repository.accounting;
import com.sacco.sacco_system.entity.accounting.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {
    Optional<FiscalPeriod> findByActiveTrue();
}