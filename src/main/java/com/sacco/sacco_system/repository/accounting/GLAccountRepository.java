package com.sacco.sacco_system.repository.accounting;

import com.sacco.sacco_system.entity.accounting.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GLAccountRepository extends JpaRepository<GLAccount, String> {
}