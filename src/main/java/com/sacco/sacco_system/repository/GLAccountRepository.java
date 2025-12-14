// Example GLAccountRepository
package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.GLAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GLAccountRepository extends JpaRepository<GLAccount, UUID> {
    Optional<GLAccount> findByCode(String code);
}