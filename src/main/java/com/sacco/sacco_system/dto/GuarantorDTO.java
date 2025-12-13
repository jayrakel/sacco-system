package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorDTO {
    private Long id;
    private Long loanId;
    private Long memberId;      // The ID of the person giving the guarantee
    private String memberName;  // Useful to show the name in the response
    private BigDecimal guaranteeAmount;
}