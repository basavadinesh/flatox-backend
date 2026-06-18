package com.flatox.backend.execption;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        String message = "Database integrity violation";
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "";
        if (rootMsg.toLowerCase().contains("users_phone_key") || rootMsg.toLowerCase().contains("phone")) {
            message = "Phone number already registered";
        } else if (rootMsg.toLowerCase().contains("users_email_key") || rootMsg.toLowerCase().contains("email")) {
            message = "Email address already registered";
        }
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
