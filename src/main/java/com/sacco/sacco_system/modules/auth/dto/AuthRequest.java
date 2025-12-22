package com.sacco.sacco_system.modules.auth.dto;

import org.apache.commons.lang3.builder.ToStringExclude;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
    @JsonProperty(value = "email")
    private String email;
    
    @JsonProperty(value = "username")
    private String username;
    
    @ToStringExclude
    private String password;
    
    // Getter that returns email or username, whichever is provided
    public String getEmailOrUsername() {
        return email != null ? email : username;
    }
}


