package com.sacco.sacco_system.entity.accounting;

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

    private LocalDateTime transactionDate;
    private LocalDateTime postedDate;

    private String description;
    private String referenceNo; // Links to your existing "TRX-..." ID

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<JournalLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if(postedDate == null) postedDate = LocalDateTime.now();
    }
}