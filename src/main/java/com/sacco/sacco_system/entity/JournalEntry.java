package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime transactionDate;
    private String description;
    private String reference; // Link to business transaction (e.g., "TRX-123")

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) transactionDate = LocalDateTime.now();
        if (lines == null) lines = new ArrayList<>();
    }
}