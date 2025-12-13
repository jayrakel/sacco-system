package com.sacco.sacco_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private Long id;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String idNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String status;
    private BigDecimal totalShares;
    private BigDecimal totalSavings;
}
