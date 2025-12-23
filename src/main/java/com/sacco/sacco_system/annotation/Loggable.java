package com.sacco.sacco_system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods or classes for automatic audit logging via AOP.
 * Use this on service methods you want to track.
 * * Example:
 * @Loggable(action = "APPROVE_LOAN", category = "LOANS")
 * public void approveLoan(UUID loanId) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE}) // ✅ Updated to allow usage on Classes (like SaccoSystemApplication)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * The action being performed (e.g., "APPROVE_LOAN").
     * Defaults to empty string (Aspect should infer from method name).
     */
    String action() default ""; // ✅ Added default value to fix compilation error
    
    /**
     * The category/entity type (e.g., "LOANS").
     * Defaults to "GENERAL".
     */
    String category() default "GENERAL"; // ✅ Added default value
}