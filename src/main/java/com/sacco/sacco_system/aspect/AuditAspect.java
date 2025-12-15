package com.sacco.sacco_system.aspect;

import com.sacco.sacco_system.annotation.Loggable;
import com.sacco.sacco_system.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(loggable)", returning = "result")
    public void logActivity(JoinPoint joinPoint, Loggable loggable, Object result) {
        try {
            // 1. Get Action Details
            String action = loggable.action();
            String category = loggable.category();

            // 2. Construct Description from Arguments (Simplified)
            // You can get fancy here and extract IDs from the result object
            String details = "Executed " + action + " in " + category;
            String entityId = "N/A";

            // If the method returns a DTO with an ID, we could extract it here using reflection
            // For now, we'll log the method signature arguments
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] != null) {
                details += " on Target ID: " + args[0].toString();
                entityId = args[0].toString();
            }

            // 3. Save Log
            auditService.logAction(action, category, entityId, details);

        } catch (Exception e) {
            System.err.println("Audit Log Failed: " + e.getMessage());
        }
    }
}