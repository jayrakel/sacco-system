package com.sacco.sacco_system.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UUID userId;
    private String username; // This is the email
    private String firstName; // âœ… NEW
    private String lastName;  // âœ… NEW
    private String memberNumber; // âœ… NEW
    private String role;
    private boolean mustChangePassword;
    private boolean systemSetupRequired;
}

