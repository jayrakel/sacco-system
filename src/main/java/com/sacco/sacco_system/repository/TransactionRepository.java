package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    // 1. For Reversals (Find by String ID like "TRX-123")
    Optional<Transaction> findByTransactionId(String transactionId);

    // 2. For Member History
    List<Transaction> findByMemberIdOrderByTransactionDateDesc(UUID memberId);

    // 3. For Savings Account History
    List<Transaction> findBySavingsAccountId(UUID savingsAccountId);

    // 4. For Filtering by Type
    List<Transaction> findByType(Transaction.TransactionType type);

    // 5. For Date Range Reports
    @Query("SELECT t FROM Transaction t WHERE t.savingsAccount.id = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    List<Transaction> findTransactionsByDateRange(UUID savingsAccountId, LocalDateTime startDate, LocalDateTime endDate);

    // 6. For Dashboard Stats (Total Registration Fees, etc.)
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = ?1")
    BigDecimal getTotalAmountByType(Transaction.TransactionType type);
}