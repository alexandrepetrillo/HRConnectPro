package com.hrconnect.employee.infrastructure.external;

import com.hrconnect.socle.exception.BusinessException;

/**
 * Exception levée lorsque la vérification du numéro de sécurité sociale échoue
 * (numéro invalide, non trouvé, etc.)
 *
 * Cette exception n'est PAS retry-able car c'est une erreur métier, pas technique.
 */
public class SecuValidationException extends BusinessException {

    public SecuValidationException(String message, String errorCode) {
        super(errorCode, message);
    }
}
