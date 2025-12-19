package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlMappingRepository extends JpaRepository<GlMapping, Long> {
    GlMapping findByTransactionTypeAndModule(String transactionType, String module);
}
