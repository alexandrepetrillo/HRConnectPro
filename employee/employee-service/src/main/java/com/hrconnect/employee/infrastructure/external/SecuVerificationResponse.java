package com.hrconnect.employee.infrastructure.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de vérification du numéro de sécurité sociale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecuVerificationResponse {
    private boolean valid;
    private String message;
    private String verificationId;
    private String errorCode;
    private String timestamp;
}
