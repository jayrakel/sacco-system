package com.sacco.sacco_system.entity.accounting;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gl_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlMapping {
    @Id
    private String eventName; // e.g., "LOAN_DISBURSEMENT", "SAVINGS_DEPOSIT"

    private String debitAccountCode;
    private String creditAccountCode;
    private String descriptionTemplate;
}