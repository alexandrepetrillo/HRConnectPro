package com.hrconnect.employee.presentation.exception;

import com.hrconnect.employee.infrastructure.external.SecuValidationException;
import com.hrconnect.employee.presentation.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions pour l'API REST.
 * Évite que les erreurs de validation ne soient redirigées vers /error
 * et ne retournent une erreur 401 au lieu de 400.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation des DTOs (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Validation failed: " + errors));
    }

    /**
     * Gère les exceptions IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Gère les exceptions de validation du numéro de sécurité sociale
     */
    @ExceptionHandler(SecuValidationException.class)
    public ResponseEntity<ErrorResponse> handleSecuValidationException(SecuValidationException ex) {
        log.warn("Secu validation error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Gère les exceptions d'accès refusé (403 Forbidden)
     * Levée par Spring Security quand l'utilisateur n'a pas les droits requis
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("Access denied: insufficient permissions"));
    }

    /**
     * Gère toutes les autres exceptions non gérées
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred"));
    }
}

