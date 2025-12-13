package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    List<SavingsAccount> findByMemberId(Long memberId);
    List<SavingsAccount> findByStatus(SavingsAccount.AccountStatus status);
    
    @Query("SELECT SUM(sa.balance) FROM SavingsAccount sa WHERE sa.status = 'ACTIVE'")
    BigDecimal getTotalActiveAccountsBalance();
    
    @Query("SELECT sa FROM SavingsAccount sa WHERE sa.member.id = ?1 AND sa.status = 'ACTIVE'")
    Optional<SavingsAccount> findActiveAccountByMemberId(Long memberId);
}
