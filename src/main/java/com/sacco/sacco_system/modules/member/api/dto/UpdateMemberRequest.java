package com.sacco.sacco_system.modules.member.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating a member
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {
    
    private String firstName;
    
    private String lastName;
    
    private String phone;
    
    private String address;
    
    private LocalDate dateOfBirth;
}




