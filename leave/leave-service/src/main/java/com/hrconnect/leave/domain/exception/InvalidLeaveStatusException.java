package com.hrconnect.leave.domain.exception;

import com.hrconnect.socle.exception.BusinessException;

/**
 * Exception levée quand le statut d'un congé ne permet pas l'opération demandée.
 */
public class InvalidLeaveStatusException extends BusinessException {

    public InvalidLeaveStatusException(String message) {
        super("INVALID_LEAVE_STATUS", message);
    }
}
