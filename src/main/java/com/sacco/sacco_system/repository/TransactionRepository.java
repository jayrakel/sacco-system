package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByMemberIdOrderByTransactionDateDesc(UUID memberId);
    List<Transaction> findBySavingsAccountId(UUID savingsAccountId);
    List<Transaction> findByType(Transaction.TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.savingsAccount.id = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    List<Transaction> findTransactionsByDateRange(UUID savingsAccountId, LocalDateTime startDate, LocalDateTime endDate);

    // ✅ NEW: Sum up all money collected for a specific type (e.g. REGISTRATION_FEE)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = ?1")
    BigDecimal getTotalAmountByType(Transaction.TransactionType type);
}