package com.hrconnect.socle.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de l'observabilité commune à tous les microservices.
 * - Métriques Prometheus
 * - Support de @Timed pour les méthodes
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Active le support de l'annotation @Timed sur les méthodes.
     * Permet de mesurer automatiquement le temps d'exécution.
     *
     * Exemple d'utilisation :
     * <pre>
     * @Timed(value = "employee.create", description = "Temps de création d'un employé")
     * public Employee createEmployee(EmployeeRequest request) { ... }
     * </pre>
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
