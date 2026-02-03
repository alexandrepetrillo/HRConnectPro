package com.hrconnect.employee.infrastructure.external;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration du RestTemplate pour les appels aux services externes
 * avec observation pour le tracing distribué
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public RestTemplateCustomizer restTemplateObservationCustomizer(ObservationRegistry observationRegistry) {
        return restTemplate -> restTemplate.setObservationRegistry(observationRegistry);
    }
}
