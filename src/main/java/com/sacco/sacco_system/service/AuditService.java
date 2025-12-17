package com.sacco.sacco_system.service;

import com.sacco.sacco_system.entity.AuditLog;
import com.sacco.sacco_system.entity.User;
import com.sacco.sacco_system.repository.AuditLogRepository;
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

        // Try to get current logged-in user
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal;
                // ✅ CHANGED: Use Full Name instead of Email (username)
                if (user.getFirstName() != null && user.getLastName() != null) {
                    username = user.getFirstName() + " " + user.getLastName();
                } else {
                    username = user.getUsername(); // Fallback if names are missing
                }
            } else {
                username = principal.toString();
            }
        } catch (Exception e) {
            // Ignore if no user context (e.g., system scheduler)
        }

        // Try to get IP Address
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