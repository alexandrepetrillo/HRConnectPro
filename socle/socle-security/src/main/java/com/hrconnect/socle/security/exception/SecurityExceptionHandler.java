package com.hrconnect.socle.security.exception;

import com.hrconnect.socle.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire des exceptions Spring Security.
 * Complète le GlobalExceptionHandler du socle-common.
 *
 * Activé automatiquement si Spring Security est dans le classpath.
 */
@Slf4j
@RestControllerAdvice
@Order(1) // Priorité haute pour intercepter avant le GlobalExceptionHandler
@ConditionalOnClass(name = "org.springframework.security.core.Authentication")
public class SecurityExceptionHandler {

    /**
     * Gestion des erreurs d'accès refusé (Spring Security).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Accès refusé : vous n'avez pas les permissions nécessaires",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gestion des erreurs d'authentification.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Authentication failed: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTHENTICATION_FAILED",
                "Échec de l'authentification",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
