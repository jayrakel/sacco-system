package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private UUID id;
    private String profileImageUrl;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String idNumber;
    private String kraPin;
    private String address;
    private LocalDate dateOfBirth;
    private String nextOfKinName;
    private String nextOfKinPhone;
    private String nextOfKinRelation;
    private String status;
    private BigDecimal totalShares;
    private BigDecimal totalSavings;
}
