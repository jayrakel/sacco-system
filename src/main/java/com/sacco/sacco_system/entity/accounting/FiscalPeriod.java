package com.sacco.sacco_system.entity.accounting;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fiscal_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name; // e.g., "FY 2024"
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private boolean closed; // If true, no transactions allowed
}