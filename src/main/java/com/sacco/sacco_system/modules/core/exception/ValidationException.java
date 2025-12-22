package com.sacco.sacco_system.modules.core.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends ApiException {
    
    public ValidationException(String message) {
        super(message, 400, "VALIDATION_ERROR");
    }
    
    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message), 400, "VALIDATION_ERROR");
    }
}


