package com.sacco.sacco_system.modules.core.exception;

/**
 * Exception thrown when a resource is not found
 */
public class ResourceNotFoundException extends ApiException {
    
    public ResourceNotFoundException(String message) {
        super(message, 404, "NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), 404, "NOT_FOUND");
    }
}


