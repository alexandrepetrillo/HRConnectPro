package com.hrconnect.payroll.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayrollService {

  private final RestTemplate restTemplate = new RestTemplate();

  public void calculer(String employeeId, int month, int year, String authHeader) {
    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(authHeader)) {
      headers.set("Authorization", authHeader);
      log.debug("Propagation du token d'authentification vers le service leave");
    }
    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(
        "http://localhost:9082/api/leaves/employee/" + employeeId,
        HttpMethod.GET,
        requestEntity,
        String.class
    );
    log.info(response.getBody());
  }
}
