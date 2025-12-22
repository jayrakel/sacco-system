package com.sacco.sacco_system.modules.audit.domain.service;

import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.repository.AuditLogRepository;
import com.sacco.sacco_system.modules.auth.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log an audit event asynchronously
     */
    @Async
    public void log(User user, String action, String entityType, String entityId, String description, AuditLog.Status status) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .userEmail(user != null ? user.getEmail() : "SYSTEM")
                    .userName(user != null ? (user.getFirstName() + " " + user.getLastName()) : "SYSTEM")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .status(status)
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {}", action, description);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    /**
     * Log a successful action
     */
    @Async
    public void logSuccess(User user, String action, String entityType, String entityId, String description) {
        log(user, action, entityType, entityId, description, AuditLog.Status.SUCCESS);
    }

    /**
     * Log a failed action
     */
    @Async
    public void logFailure(User user, String action, String entityType, String entityId, String description, String errorMessage) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .userEmail(user != null ? user.getEmail() : "SYSTEM")
                    .userName(user != null ? (user.getFirstName() + " " + user.getLastName()) : "SYSTEM")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .status(AuditLog.Status.FAILURE)
                    .errorMessage(errorMessage)
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log (FAILURE) created: {} - {}", action, description);
        } catch (Exception e) {
            log.error("Failed to create failure audit log: {}", e.getMessage());
        }
    }

    /**
     * Log system actions (no user)
     */
    @Async
    public void logSystem(String action, String entityType, String entityId, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userEmail("SYSTEM")
                    .userName("SYSTEM")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .status(AuditLog.Status.SUCCESS)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("System audit log created: {} - {}", action, description);
        } catch (Exception e) {
            log.error("Failed to create system audit log: {}", e.getMessage());
        }
    }

    // === Query Methods ===

    public Page<AuditLog> getAuditLogsForUser(User user, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }

    public List<AuditLog> getAuditLogsForEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    public Page<AuditLog> getFailedAuditLogs(Pageable pageable) {
        return auditLogRepository.findByStatusOrderByCreatedAtDesc(AuditLog.Status.FAILURE, pageable);
    }

    public Page<AuditLog> searchAuditLogs(String search, Pageable pageable) {
        return auditLogRepository.searchAuditLogs(search, pageable);
    }

    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    // === Helper Methods ===

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        return request.getHeader("User-Agent");
    }
}
