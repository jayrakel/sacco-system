package com.sacco.sacco_system.aspect;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ Import
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j // ✅ Annotation
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(loggable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            String action = loggable.action();
            String category = loggable.category();

            String details = "Executed " + action + " in " + category;
            String entityId = "N/A";

            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                details += " on Target ID: " + args[0].toString();
                entityId = args[0].toString();
            }

            auditService.logAction(action, category, entityId, details);

        } catch (Exception e) {
            log.error("Audit Log Failed: {}", e.getMessage()); // ✅ Replaced Syserr
        }
    }
}