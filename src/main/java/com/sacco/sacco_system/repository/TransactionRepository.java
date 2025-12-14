package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID; // ✅ Import UUID

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // ✅ FIX: Changed Long to UUID
    List<Transaction> findBySavingsAccountId(UUID savingsAccountId);

    List<Transaction> findByType(Transaction.TransactionType type);

    // ✅ FIX: Changed Long to UUID
    @Query("SELECT t FROM Transaction t WHERE t.savingsAccount.id = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    List<Transaction> findTransactionsByDateRange(UUID savingsAccountId, LocalDateTime startDate, LocalDateTime endDate);
}