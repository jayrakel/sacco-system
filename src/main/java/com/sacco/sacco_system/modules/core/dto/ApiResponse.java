package com.sacco.sacco_system.modules.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't send null fields (keeps JSON clean)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // ✅ Added to support GlobalExceptionHandler
    private Integer statusCode;
    private String path;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // --- CONSTRUCTORS ---

    // 1. Used by LOAN MODULE (Success with Data)
    // Usage: new ApiResponse(true, "Message", data)
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.statusCode = success ? 200 : 400; // Default status
        this.timestamp = LocalDateTime.now();
    }

    // 2. Used by LOAN MODULE (Success without Data)
    // Usage: new ApiResponse(true, "Message")
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.statusCode = success ? 200 : 400;
        this.timestamp = LocalDateTime.now();
    }

    // 3. Used by GLOBAL EXCEPTION HANDLER (Error Handling)
    // Usage: new ApiResponse("Error Message", 404)
    public ApiResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.success = statusCode >= 200 && statusCode < 300; // Auto-calculate success
        this.timestamp = LocalDateTime.now();
    }
}