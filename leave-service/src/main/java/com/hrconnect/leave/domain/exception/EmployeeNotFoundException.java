package com.hrconnect.leave.domain.exception;

import com.hrconnect.socle.exception.BusinessException;

/**
 * Exception levée quand un employé n'est pas trouvé dans la projection locale.
 */
public class EmployeeNotFoundException extends BusinessException {

    public EmployeeNotFoundException(String employeeId) {
        super("EMPLOYEE_NOT_FOUND", "Employee not found: " + employeeId);
    }
}
