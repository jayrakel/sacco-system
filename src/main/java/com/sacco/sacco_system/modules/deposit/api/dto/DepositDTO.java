package com.sacco.sacco_system.modules.deposit.api.dto;

import com.sacco.sacco_system.modules.deposit.domain.entity.DepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for deposit response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositDTO {

    private UUID id;
    private UUID memberId;
    private String memberName;
    private BigDecimal totalAmount;
    private DepositStatus status;
    private String transactionReference;
    private String paymentMethod;
    private String paymentReference;
    private List<AllocationDTO> allocations;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
