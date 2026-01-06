package com.sacco.sacco_system.modules.finance.domain.entity.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "journal_entry_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private JournalEntry journalEntry;

    // Renamed from account -> glAccount
    @ManyToOne
    @JoinColumn(name = "gl_account_id", nullable = false)
    private GLAccount glAccount;

    @Column(nullable = false)
    private String description; // Line-level description

    // Renamed from debit -> debitAmount
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    // Renamed from credit -> creditAmount
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;
}