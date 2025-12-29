package com.sacco.sacco_system.modules.admin.api.controller.systemsetting.handler;

import com.sacco.sacco_system.modules.admin.domain.service.systemsetting.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SystemSettingReadHandler {

    private final SystemSettingService service;

    public ResponseEntity<?> getAllSettings() {
        return ResponseEntity.ok(Map.of("success", true, "data", service.getAllSettings()));
    }
}