package com.hrconnect.employee.infrastructure.external;

/**
 * Exception levée lorsque la vérification du numéro de sécurité sociale échoue
 * (numéro invalide, non trouvé, etc.)
 *
 * Cette exception n'est PAS retry-able car c'est une erreur métier, pas technique.
 */
public class SecuValidationException extends RuntimeException {

    private final String errorCode;

    public SecuValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
