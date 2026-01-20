package com.hrconnect.leave.domain.exception;

import com.hrconnect.socle.exception.BusinessException;

/**
 * Exception levée quand une demande de congé n'est pas trouvée.
 */
public class LeaveNotFoundException extends BusinessException {

    public LeaveNotFoundException(String leaveId) {
        super("LEAVE_NOT_FOUND", "Leave request not found: " + leaveId);
    }
}
