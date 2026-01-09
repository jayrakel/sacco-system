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

    // --- 1. BASIC LOOKUPS ---
    @Query("SELECT s FROM SavingsAccount s WHERE s.member.id = :memberId")
    List<SavingsAccount> findByMemberId(@Param("memberId") UUID memberId);

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    List<SavingsAccount> findByAccountStatus(SavingsAccount.AccountStatus accountStatus);

    // --- 2. FOR LOAN MODULE (Single Member Total) ---
    // Calculates total savings for ONE member (used for eligibility)
    @Query("SELECT COALESCE(SUM(s.balanceAmount), 0) FROM SavingsAccount s WHERE s.member.id = :memberId AND s.accountStatus = 'ACTIVE'")
    BigDecimal getTotalSavings(@Param("memberId") UUID memberId);

    // --- 3. FOR FINANCIAL REPORTS (System Wide Total) ---
    // Calculates total savings for the ENTIRE SACCO (used for the dashboard)
    // We renamed this to 'getTotalActiveAccountsBalance' to match your FinancialReportService
    @Query("SELECT COALESCE(SUM(s.balanceAmount), 0) FROM SavingsAccount s WHERE s.accountStatus = 'ACTIVE'")
    BigDecimal getTotalActiveAccountsBalance();

    // Helper for fetching a member's main account
    @Query("SELECT s FROM SavingsAccount s WHERE s.member.id = :memberId AND s.accountStatus = 'ACTIVE'")
    Optional<SavingsAccount> findActiveAccountByMemberId(@Param("memberId") UUID memberId);
}