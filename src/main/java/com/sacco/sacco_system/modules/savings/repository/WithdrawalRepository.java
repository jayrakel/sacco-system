package com.sacco.sacco_system.modules.savings.repository;

import com.sacco.sacco_system.modules.savings.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    List<Withdrawal> findByMemberId(Long memberId);
    List<Withdrawal> findByStatus(Withdrawal.WithdrawalStatus status);
    List<Withdrawal> findBySavingsAccountId(Long savingsAccountId);
    
    @Query("SELECT SUM(w.amount) FROM Withdrawal w WHERE w.status = 'PROCESSED'")
    BigDecimal getTotalWithdrawals();
}
