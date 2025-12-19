package com.sacco.sacco_system.modules.system.dto;

import com.sacco.sacco_system.modules.auth.model.User;
import lombok.Data;
import java.util.List;

@Data
public class SetupRequest {
    private List<NewAdmin> admins;

    @Data
    public static class NewAdmin {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private User.Role role;
    }
}