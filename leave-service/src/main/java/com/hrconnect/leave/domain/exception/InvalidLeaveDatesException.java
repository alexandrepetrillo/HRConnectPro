package com.hrconnect.leave.domain.exception;

import com.hrconnect.socle.exception.BusinessException;

/**
 * Exception levée quand les dates d'un congé sont invalides.
 */
public class InvalidLeaveDatesException extends BusinessException {

    public InvalidLeaveDatesException(String message) {
        super("INVALID_LEAVE_DATES", message);
    }
}
