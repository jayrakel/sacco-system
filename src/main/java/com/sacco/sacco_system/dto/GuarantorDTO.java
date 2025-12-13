package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorDTO {
    private UUID id;
    private UUID loanId;
    private UUID memberId;      // The ID of the person giving the guarantee
    private String memberName;  // Useful to show the name in the response
    private BigDecimal guaranteeAmount;
}