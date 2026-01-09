package com.sacco.sacco_system.modules.member.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeneficiaryDTO {
    private String firstName;
    private String lastName;
    private String relationship;
    private String identityNumber;
    private String phoneNumber;
    private Double allocationPercentage; // Percentage
}