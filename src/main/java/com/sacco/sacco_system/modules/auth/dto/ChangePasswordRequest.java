package com.sacco.sacco_system.modules.auth.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmationPassword;
}