package com.sacco.sacco_system.modules.core.exception;

/**
 * Custom API exception for application-specific errors
 */
public class ApiException extends RuntimeException {
    
    private final int statusCode;
    private final String errorCode;
    
    public ApiException(String message) {
        this(message, 400, "BAD_REQUEST");
    }
    
    public ApiException(String message, int statusCode) {
        this(message, statusCode, "ERROR");
    }
    
    public ApiException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}


