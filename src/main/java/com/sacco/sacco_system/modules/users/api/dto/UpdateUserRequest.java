package com.sacco.sacco_system.modules.users.api.dto;

import com.sacco.sacco_system.modules.users.domain.entity.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    
    private String firstName;
    private String lastName;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private String officialEmail;
    private String phoneNumber;
    private User.Role role;
    private Boolean enabled;
    private Boolean emailVerified;
}
