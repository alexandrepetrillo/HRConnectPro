package com.hrconnect.employee.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client REST pour communiquer avec le Leave-Service
 */
@Component
@Slf4j
public class LeaveServiceClient {

    private final RestTemplate restTemplate;
    private final String leaveServiceUrl;

    public LeaveServiceClient(
            RestTemplate restTemplate,
            @Value("${leave-service.url:http://localhost:9082}") String leaveServiceUrl) {
        this.restTemplate = restTemplate;
        this.leaveServiceUrl = leaveServiceUrl;
    }

    /**
     * Initialise les compteurs de congés pour un nouvel employé
     */
    public LeaveBalanceResponse initializeLeaveBalance(String employeeId, Integer cpAnnuels, Integer rttAnnuels) {
        log.info("Calling Leave-Service to initialize balance for employee: {}", employeeId);

        try {
            String url = leaveServiceUrl + "/api/leave-balances/initialize";

            InitializeLeaveBalanceRequest request = InitializeLeaveBalanceRequest.builder()
                    .employeeId(employeeId)
                    .cpAnnuels(cpAnnuels)
                    .rttAnnuels(rttAnnuels)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<InitializeLeaveBalanceRequest> entity = new HttpEntity<>(request, headers);

            LeaveBalanceResponse response = restTemplate.postForObject(url, entity, LeaveBalanceResponse.class);

            log.info("Successfully initialized leave balance for employee: {}", employeeId);
            return response;

        } catch (Exception e) {
            log.error("Failed to initialize leave balance for employee: {}", employeeId, e);
            // On ne propage pas l'exception pour ne pas bloquer la création de l'employé
            // L'initialisation pourra être refaite manuellement si nécessaire
            log.warn("Leave balance initialization failed but employee creation will continue");
            return null;
        }
    }
}
