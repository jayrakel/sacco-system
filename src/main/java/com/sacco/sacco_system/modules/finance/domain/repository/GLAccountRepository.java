package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, String> {

    Optional<GLAccount> findByCode(String code);

    // ✅ NEW: Efficiently fetch all Liquid Asset accounts (e.g., "10%")
    List<GLAccount> findByCodeStartingWith(String prefix);
}
