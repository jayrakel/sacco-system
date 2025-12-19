package com.sacco.sacco_system.modules.accounting.model;

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
    @JoinColumn(name = "journal_entry_id")
    @JsonIgnore // ✅ FIX: Stops infinite loop during JSON conversion
    @ToString.Exclude // ✅ FIX: Stops infinite loop in Logs
    private JournalEntry journalEntry;

    @ManyToOne
    @JoinColumn(name = "account_code")
    private GLAccount account;

    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
}