package com.sacco.sacco_system.modules.deposit.api.dto;

import com.sacco.sacco_system.modules.deposit.domain.entity.DepositProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Deposit Product
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositProductDTO {

    private UUID id;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @Positive(message = "Target amount must be positive")
    private BigDecimal targetAmount;

    private BigDecimal currentAmount;

    private DepositProductStatus status;

    private UUID createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
