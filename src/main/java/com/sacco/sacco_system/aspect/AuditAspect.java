package com.sacco.sacco_system.aspect;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.modules.audit.domain.entity.AuditLog;
import com.sacco.sacco_system.modules.audit.domain.service.AuditService;
import com.sacco.sacco_system.modules.users.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Log successful method execution
     */
    @AfterReturning(pointcut = "@annotation(loggable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            User user = getCurrentUser();
            String action = loggable.action();
            String category = loggable.category();

            // Extract entity details
            StringBuilder description = new StringBuilder("Executed " + action + " in " + category);
            String entityId = extractEntityId(joinPoint.getArgs(), result);

            // Add argument details
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                String argString = args[0].toString();
                if (argString.length() > 50 || argString.contains("=")) {
                    description.append(" | Payload: ").append(truncate(argString, 200));
                } else {
                    description.append(" | Target ID: ").append(argString);
                }
            }

            // Log success
            auditService.logSuccess(user, action, category, entityId, description.toString());

        } catch (Exception e) {
            log.error("Failed to log audit activity: {}", e.getMessage());
        }
    }

    /**
     * Log failed method execution
     */
    @AfterThrowing(pointcut = "@annotation(loggable)", throwing = "exception")
    public void logFailure(JoinPoint joinPoint, Loggable loggable, Throwable exception) {
        try {
            User user = getCurrentUser();
            String action = loggable.action();
            String category = loggable.category();

            // Extract entity details
            String entityId = extractEntityId(joinPoint.getArgs(), null);
            String description = "Failed to execute " + action + " in " + category;

            // Log failure
            auditService.logFailure(user, action, category, entityId, description, exception.getMessage());

        } catch (Exception e) {
            log.error("Failed to log audit failure: {}", e.getMessage());
        }
    }

    /**
     * Extract entity ID from method arguments or result
     */
    private String extractEntityId(Object[] args, Object result) {
        // Try to get ID from result first (for CREATE operations)
        if (result != null) {
            String resultStr = result.toString();
            if (resultStr.length() < 50 && !resultStr.contains("=")) {
                return resultStr;
            }
        }

        // Try to get ID from first argument
        if (args.length > 0 && args[0] != null) {
            String argString = args[0].toString();
            if (argString.length() < 50 && !argString.contains("=")) {
                return argString;
            }
        }

        return "N/A";
    }

    /**
     * Get currently authenticated user
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return (User) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("No authenticated user found: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Truncate string to max length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }
}
