package com.sacco.sacco_system.modules.deposit.api.dto;

import com.sacco.sacco_system.modules.deposit.domain.entity.DepositDestinationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for deposit allocation request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationRequest {

    @NotNull(message = "Destination type is required")
    private DepositDestinationType destinationType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    // One of these will be set based on destinationType
    private UUID savingsAccountId;
    private UUID loanId;
    private UUID fineId;
    private UUID depositProductId;

    private String notes;
}
