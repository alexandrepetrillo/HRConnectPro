package com.hrconnect.employee.presentation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hrconnect.socle.exception.ErrorResponse;

/**
 * Gestionnaire des exceptions SPÉCIFIQUES à employee-service.
 * Les exceptions génériques (validation, BusinessException, etc.) sont gérées par le socle.
 *
 * @Order(1) pour que ce handler soit prioritaire sur celui du socle pour ces exceptions spécifiques.
 */
@RestControllerAdvice
@Order(1)
@Slf4j
public class EmployeeExceptionHandler {

    /**
     * Gère les exceptions d'accès refusé (403 Forbidden)
     * Levée par Spring Security quand l'utilisateur n'a pas les droits requis
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access denied: insufficient permissions",
                null
            ));
    }
}
