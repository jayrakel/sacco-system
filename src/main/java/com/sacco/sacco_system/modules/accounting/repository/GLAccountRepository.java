package com.sacco.sacco_system.modules.accounting.repository;

import com.sacco.sacco_system.modules.accounting.model.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, String> {
}