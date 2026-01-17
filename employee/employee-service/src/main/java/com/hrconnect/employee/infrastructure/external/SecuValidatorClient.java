package com.hrconnect.employee.infrastructure.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

/**
 * Client pour l'API de vérification du numéro de sécurité sociale.
 *
 * Utilise Resilience4j pour la tolérance aux pannes :
 * - CircuitBreaker : évite d'appeler un service défaillant
 * - Retry : réessaie automatiquement en cas d'erreur temporaire
 * - TimeLimiter : timeout si le service est trop lent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecuValidatorClient {

    private final RestTemplate restTemplate;

    @Value("${external.secu-validator.url}")
    private String secuValidatorUrl;

    /**
     * Vérifie la validité d'un numéro de sécurité sociale auprès du service externe.
     *
     * @param numeroSecuriteSociale Le numéro à vérifier
     * @param nom Le nom de famille
     * @param dateNaissance La date de naissance
     * @return true si le numéro est valide, false sinon
     */
    public SecuVerificationResponse verify(String numeroSecuriteSociale, String nom,
                                            LocalDate dateNaissance) {
        log.info("Vérification du numéro de sécurité sociale: {}****",
                 numeroSecuriteSociale.substring(0, 5));

        SecuVerificationRequest request = SecuVerificationRequest.builder()
                .numeroSecuriteSociale(numeroSecuriteSociale)
                .nom(nom.toUpperCase())
                .dateNaissance(dateNaissance)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<SecuVerificationRequest> entity = new HttpEntity<>(request, headers);

        SecuVerificationResponse response = restTemplate.postForObject(
                secuValidatorUrl + "/api/v1/verify",
                entity,
                SecuVerificationResponse.class
        );

        log.info("Résultat vérification: valid={}, message={}",
                 response.isValid(), response.getMessage());

        return response;
    }
}
