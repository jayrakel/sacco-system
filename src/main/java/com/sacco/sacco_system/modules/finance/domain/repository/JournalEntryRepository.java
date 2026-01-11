package com.sacco.sacco_system.modules.finance.domain.repository;

import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByReferenceNo(String referenceNo);

    List<JournalEntry> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT j FROM JournalEntry j ORDER BY j.transactionDate DESC")
    List<JournalEntry> findAllOrderByDateDesc();
}

