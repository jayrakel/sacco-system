package com.sacco.sacco_system.modules.finance.domain.entity.accounting;

import jakarta.persistence.*;
import lombok.*;
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

    // Domain Dictionary: Accounting Date
    @Column(nullable = false)
    private LocalDateTime entryDate; // Renamed from transactionDate

    @Column(nullable = false)
    private String description; // Header-level narration

    // Linkage: Matches Transaction.transactionReference
    @Column(nullable = false, unique = true)
    private String transactionReference; // Renamed from referenceNo

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JournalEntryStatus status = JournalEntryStatus.POSTED;

    private LocalDateTime postedAt; // Renamed from postedDate

    // Parent-Child Relationship
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    // Global Audit
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Helper to enforce Bidirectional consistency
    public void addLine(JournalLine line) {
        lines.add(line);
        line.setJournalEntry(this);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (entryDate == null) entryDate = LocalDateTime.now();
        if (postedAt == null) postedAt = LocalDateTime.now();
        if (status == null) status = JournalEntryStatus.POSTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum JournalEntryStatus {
        DRAFT, POSTED, REVERSED
    }
}