package com.sacco.sacco_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne
    @JoinColumn(name = "gl_account_id", nullable = false)
    private GLAccount account;

    private BigDecimal debit;  // Amount if debit
    private BigDecimal credit; // Amount if credit

    // Helper: Validates that either debit or credit is set, not both
    @PrePersist
    @PreUpdate
    public void validate() {
        if (debit == null) debit = BigDecimal.ZERO;
        if (credit == null) credit = BigDecimal.ZERO;
    }
}