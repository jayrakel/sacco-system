package com.sacco.sacco_system.repository;

import com.sacco.sacco_system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findBySavingsAccountId(Long savingsAccountId);
    List<Transaction> findByType(Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t WHERE t.savingsAccount.id = ?1 AND t.transactionDate BETWEEN ?2 AND ?3")
    List<Transaction> findTransactionsByDateRange(Long savingsAccountId, LocalDateTime startDate, LocalDateTime endDate);
}
