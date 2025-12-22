package com.sacco.sacco_system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for automatic audit logging via AOP.
 * Use this on service methods you want to track.
 * 
 * Example:
 * @Loggable(action = "APPROVE_LOAN", category = "LOANS")
 * public void approveLoan(UUID loanId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * The action being performed (e.g., "APPROVE_LOAN", "CREATE_MEMBER")
     * Use AuditLog.Actions constants when possible
     */
    String action();
    
    /**
     * The category/entity type (e.g., "LOANS", "MEMBERS", "TRANSACTIONS")
     */
    String category();
}
