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
    private String fullName;
    private String relationship;
    private String idNumber;
    private String phoneNumber;
    private Double allocation; // Percentage
}