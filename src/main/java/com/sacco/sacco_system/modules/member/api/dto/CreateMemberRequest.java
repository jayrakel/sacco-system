package com.sacco.sacco_system.modules.member.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;

/**
 * Request DTO for creating a new member
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequest {
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Phone is required")
    private String phone;
    
    @NotBlank(message = "National ID is required")
    private String nationalId;

    private String address;
    
    private LocalDate dateOfBirth;
}




