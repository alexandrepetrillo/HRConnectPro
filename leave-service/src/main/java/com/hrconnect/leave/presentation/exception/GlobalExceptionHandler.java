package com.hrconnect.leave.presentation.exception;

import com.hrconnect.leave.application.service.LeaveService.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'API REST
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        log.warn("Employee not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Employee Not Found", ex.getMessage());
    }

    @ExceptionHandler(LeaveNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLeaveNotFound(LeaveNotFoundException ex) {
        log.warn("Leave not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Leave Not Found", ex.getMessage());
    }

    @ExceptionHandler(InvalidLeaveDatesException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDates(InvalidLeaveDatesException ex) {
        log.warn("Invalid leave dates: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Dates", ex.getMessage());
    }

    @ExceptionHandler(InvalidLeaveStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(InvalidLeaveStatusException ex) {
        log.warn("Invalid leave status: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Status", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

