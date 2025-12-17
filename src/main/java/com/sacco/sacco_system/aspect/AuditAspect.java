package com.sacco.sacco_system.aspect;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(loggable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            String action = loggable.action();
            String category = loggable.category();

            // Start with base details
            StringBuilder detailsBuffer = new StringBuilder("Executed " + action + " in " + category);
            String entityId = "N/A";

            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                String argString = args[0].toString();

                // ✅ FIX: Check length. If > 50, it's likely an Object DTO, not an ID.
                if (argString.length() > 50 || argString.contains("=")) {
                    // It's a complex object (like MemberDTO), don't put in entityId
                    detailsBuffer.append(" | Payload: ").append(argString);
                    entityId = "New/Complex Object";
                } else {
                    // It's likely a simple ID (UUID or String)
                    detailsBuffer.append(" on Target ID: ").append(argString);
                    entityId = argString;
                }
            }

            // ✅ OPTIONAL: If result has an ID, use it (for Create actions)
            if (result != null && result.toString().length() < 50) {
                // Simple heuristic: if result is small (like a UUID return), use it
                // If result is a DTO, we skip to avoid huge strings again
            }

            // Safety Truncate just in case
            if (entityId.length() > 255) {
                entityId = entityId.substring(0, 255);
            }

            auditService.logAction(action, category, entityId, detailsBuffer.toString());

        } catch (Exception e) {
            log.error("Audit Log Failed: {}", e.getMessage());
        }
    }
}