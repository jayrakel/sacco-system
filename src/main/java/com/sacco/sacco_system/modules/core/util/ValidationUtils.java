package com.sacco.sacco_system.modules.core.util;

import com.sacco.sacco_system.modules.core.exception.ValidationException;

/**
 * Utility class for common validation operations
 */
public class ValidationUtils {
    
    public static void validateNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new ValidationException(fieldName, "cannot be null");
        }
    }
    
    public static void validateNotEmpty(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new ValidationException(fieldName, "cannot be empty");
        }
    }
    
    public static void validatePositive(Number number, String fieldName) {
        if (number == null || number.doubleValue() <= 0) {
            throw new ValidationException(fieldName, "must be positive");
        }
    }
    
    public static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("email", "invalid email format");
        }
    }
    
    public static void validateTrue(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}


