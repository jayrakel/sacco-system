package com.sacco.sacco_system.modules.accounting.repository;

import com.sacco.sacco_system.modules.accounting.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
}