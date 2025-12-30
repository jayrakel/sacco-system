package com.sacco.sacco_system.modules.core.exception;

import com.sacco.sacco_system.modules.core.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // ✅ Added Import
import org.springframework.security.core.Authentication; // ✅ Added Import
import org.springframework.security.core.context.SecurityContextHolder; // ✅ Added Import
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ NEW: Handle Access Denied (Auth Errors)
    // This logs EXACTLY who tried to access what, and what roles they had.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userDetails = (auth != null)
                ? "User: " + auth.getName() + " | Authorities: " + auth.getAuthorities()
                : "Unknown User";

        log.error("⛔ Access Denied for [{}]. Reason: {}", userDetails, ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>("Access Denied: You do not have the required role.", 403);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex, WebRequest request) {
        log.error("API Exception: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(ex.getMessage(), ex.getStatusCode());
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getStatusCode()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(ex.getMessage(), 404);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(ex.getMessage(), 400);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Object> response = new ApiResponse<>("Validation failed", 400);
        response.setData(errors);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected exception: ", ex);
        ApiResponse<Object> response = new ApiResponse<>("An unexpected error occurred: " + ex.getMessage(), 500);
        response.setPath(request.getDescription(false).replace("uri=", ""));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}