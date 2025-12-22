package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlMappingRepository extends JpaRepository<GlMapping, String> {
    // Find GL mapping by event name (e.g., "LOAN_DISBURSEMENT", "SAVINGS_DEPOSIT")
    Optional<GlMapping> findByEventName(String eventName);
}
