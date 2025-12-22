package com.sacco.sacco_system.modules.deposit.api.dto;

import com.sacco.sacco_system.modules.deposit.domain.entity.AllocationStatus;
import com.sacco.sacco_system.modules.deposit.domain.entity.DepositDestinationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for deposit allocation response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationDTO {

    private UUID id;
    private DepositDestinationType destinationType;
    private BigDecimal amount;
    private AllocationStatus status;
    private String destinationName;  // Friendly name of destination
    private String notes;
    private String errorMessage;
}
