package com.sacco.sacco_system.modules.savings.domain.repository;

import com.sacco.sacco_system.modules.savings.domain.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {

    // ✅ FIX: Use @Query to map the method name to the correct entity field (s.member.id)
    @Query("SELECT s FROM SavingsAccount s WHERE s.member.id = :memberId")
    List<SavingsAccount> findByMemberId(@Param("memberId") UUID memberId);

    // Also keep this version if your LoanService uses the underscore syntax
    @Query("SELECT s FROM SavingsAccount s WHERE s.member.id = :memberId")
    List<SavingsAccount> findByMember_Id(@Param("memberId") UUID memberId);

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    List<SavingsAccount> findByStatus(SavingsAccount.AccountStatus status);

    @Query("SELECT SUM(sa.balance) FROM SavingsAccount sa WHERE sa.status = 'ACTIVE'")
    BigDecimal getTotalActiveAccountsBalance();

    @Query("SELECT sa FROM SavingsAccount sa WHERE sa.member.id = ?1 AND sa.status = 'ACTIVE'")
    Optional<SavingsAccount> findActiveAccountByMemberId(UUID memberId);
}