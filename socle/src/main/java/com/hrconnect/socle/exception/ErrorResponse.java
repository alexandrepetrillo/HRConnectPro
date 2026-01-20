package com.hrconnect.socle.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Réponse d'erreur standardisée pour toutes les APIs.
 * Format uniforme de gestion des erreurs dans tous les microservices.
 */
@Getter
@Builder
public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String code;
    private final String message;
    private final String details;
    private final String path;

    public static ErrorResponse of(int status, String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    public static ErrorResponse of(int status, String code, String message, String details, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .code(code)
                .message(message)
                .details(details)
                .path(path)
                .build();
    }
}
