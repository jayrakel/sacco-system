package com.sacco.sacco_system.modules.finance.domain.entity.accounting;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.GLAccount;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalEntry;
import com.sacco.sacco_system.modules.finance.domain.entity.accounting.JournalLine;

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
    @JsonIgnore // âœ… FIX: Stops infinite loop during JSON conversion
    @ToString.Exclude // âœ… FIX: Stops infinite loop in Logs
    private JournalEntry journalEntry;

    @ManyToOne
    @JoinColumn(name = "account_code")
    private GLAccount account;

    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
}





