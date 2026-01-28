package com.hrconnect.employee.presentation.controller.exception;

import lombok.Getter;

/**
 * Exception métier de base pour tous les microservices.
 * Permet de standardiser la gestion des erreurs fonctionnelles.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final String details;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = null;
    }
}