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
    private String firstName;
    private String lastName;
    private String memberNumber;
    private String role;
    private boolean mustChangePassword;
    private boolean systemSetupRequired;

    // NEW: Dual login indicators
    private boolean isOfficialLogin; // True if logged in with official SACCO email
    private boolean isMemberLogin;   // True if logged in with personal email
}

