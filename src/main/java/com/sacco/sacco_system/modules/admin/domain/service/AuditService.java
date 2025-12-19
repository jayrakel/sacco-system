package com.sacco.sacco_system.modules.admin.domain.service;

import com.sacco.sacco_system.modules.admin.domain.entity.AuditLog;
// âœ… FIX: Import User from the new Auth Module
import com.sacco.sacco_system.modules.auth.model.User;
import com.sacco.sacco_system.modules.admin.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String action, String entityName, String entityId, String details) {
        String username = "System";
        String ipAddress = "Unknown";

        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal;
                // Use Full Name if available, else username (email)
                if (user.getFirstName() != null && user.getLastName() != null) {
                    username = user.getFirstName() + " " + user.getLastName();
                } else {
                    username = user.getUsername();
                }
            } else {
                username = principal.toString();
            }
        } catch (Exception e) {
            // No user logged in (e.g. registration or background task)
        }

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignore
        }

        AuditLog log = AuditLog.builder()
                .username(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(log);
    }
}



