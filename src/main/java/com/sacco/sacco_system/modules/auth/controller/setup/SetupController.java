package com.sacco.sacco_system.modules.auth.controller.setup;

import com.sacco.sacco_system.modules.auth.service.setup.SetupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final SetupService setupService;

    // DTOs defined here for convenience, or move to dedicated DTO package
    @Data
    public static class CriticalAdminRequest {
        private String role;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
    }

    @Data
    public static class CriticalAdminsPayload {
        private List<CriticalAdminRequest> admins;
    }

    /**
     * Create critical admin users and initialize system
     */
    @PostMapping("/critical-admins")
    public ResponseEntity<?> createCriticalAdmins(@RequestBody CriticalAdminsPayload payload) {
        log.info("🚀 Received System Setup Request with {} admins", payload.getAdmins().size());

        var response = setupService.performSystemSetup(payload.getAdmins());

        if (Boolean.FALSE.equals(response.get("success"))) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}