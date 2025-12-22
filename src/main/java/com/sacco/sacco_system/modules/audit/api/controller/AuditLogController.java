package com.sacco.sacco_system.modules.audit.api.controller;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditService auditService;

    /**
     * Get all audit logs (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getAllAuditLogs(pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs.getContent(),
            "currentPage", auditLogs.getNumber(),
            "totalItems", auditLogs.getTotalElements(),
            "totalPages", auditLogs.getTotalPages()
        ));
    }

    /**
     * Get audit logs for current user
     */
    @GetMapping("/my-logs")
    public ResponseEntity<?> getMyAuditLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getAuditLogsForUser(user, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs.getContent(),
            "currentPage", auditLogs.getNumber(),
            "totalItems", auditLogs.getTotalElements(),
            "totalPages", auditLogs.getTotalPages()
        ));
    }

    /**
     * Search audit logs
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchAuditLogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.searchAuditLogs(query, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs.getContent(),
            "currentPage", auditLogs.getNumber(),
            "totalItems", auditLogs.getTotalElements(),
            "totalPages", auditLogs.getTotalPages()
        ));
    }

    /**
     * Get audit logs by date range
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(startDate, endDate, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs.getContent(),
            "currentPage", auditLogs.getNumber(),
            "totalItems", auditLogs.getTotalElements(),
            "totalPages", auditLogs.getTotalPages()
        ));
    }

    /**
     * Get audit logs for specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHAIRPERSON', 'SECRETARY')")
    public ResponseEntity<?> getAuditLogsForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        
        List<AuditLog> auditLogs = auditService.getAuditLogsForEntity(entityType, entityId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs
        ));
    }

    /**
     * Get failed audit logs
     */
    @GetMapping("/failures")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getFailedAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getFailedAuditLogs(pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", auditLogs.getContent(),
            "currentPage", auditLogs.getNumber(),
            "totalItems", auditLogs.getTotalElements(),
            "totalPages", auditLogs.getTotalPages()
        ));
    }
}
