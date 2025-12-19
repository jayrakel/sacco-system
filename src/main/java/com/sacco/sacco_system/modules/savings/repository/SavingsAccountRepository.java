package com.sacco.sacco_system.modules.savings.repository;

import com.sacco.sacco_system.modules.savings.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // ✅ Import UUID

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    // ✅ Use UUID here to match Member.java
    List<SavingsAccount> findByMemberId(UUID memberId);

    List<SavingsAccount> findByStatus(SavingsAccount.AccountStatus status);

    // ✅ Fix: Use AccountStatus enum in query (or string 'ACTIVE')
    @Query("SELECT SUM(sa.balance) FROM SavingsAccount sa WHERE sa.status = 'ACTIVE'")
    BigDecimal getTotalActiveAccountsBalance();

    // ✅ FIX: Changed Long to UUID to match Member Entity
    @Query("SELECT sa FROM SavingsAccount sa WHERE sa.member.id = ?1 AND sa.status = 'ACTIVE'")
    Optional<SavingsAccount> findActiveAccountByMemberId(UUID memberId);
}