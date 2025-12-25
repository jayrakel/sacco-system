package com.sacco.sacco_system.modules.deposit.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating a new deposit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDepositRequest {

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be greater than zero")
    private BigDecimal totalAmount;

    @NotEmpty(message = "At least one allocation is required")
    @Valid
    private List<AllocationRequest> allocations;

    private String paymentMethod;  // MPESA, BANK, CASH

    private String paymentReference;  // External reference

    // ✅ ADDED: Capture the specific bank GL Code (e.g., "1010" for Equity)
    private String bankAccountCode; 

    private String notes;
}