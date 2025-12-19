package com.sacco.sacco_system.modules.member.api.dto;

import com.sacco.sacco_system.modules.member.domain.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for member
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    
    private Long id;
    
    private String memberNumber;
    
    private String firstName;
    
    private String lastName;
    
    private String fullName;
    
    private String email;
    
    private String phone;
    
    private String idNumber;
    
    private String address;
    
    private LocalDate dateOfBirth;
    
    private MemberStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}




