package com.sacco.sacco_system.modules.loan.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String relationship;
    private String idNumber;
}
