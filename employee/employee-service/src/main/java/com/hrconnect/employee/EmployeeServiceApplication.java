package com.hrconnect.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Employee Service - Microservice de gestion des employés
 *
 * Responsabilités :
 * - Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel (base)
 * - Publication des snapshots Employee sur le topic employee.state
 *
 * Note : @ComponentScan inclut les packages du socle technique pour que les beans
 * (JwtAuthenticationFilter, JwtTokenProvider, etc.) soient disponibles.
 *
 * Les annotations @EntityScan et @EnableJpaRepositories incluent les packages DLQ
 * du socle pour la gestion des messages en erreur.
 */

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hrconnect.employee",      // Package de l'application
    "com.hrconnect.socle"          // Package du socle technique
})
@EntityScan(basePackages = {
    "com.hrconnect.employee",      // Entités de l'application
    "com.hrconnect.socle.kafka.dlq" // Entité DlqMessage du socle
})
@EnableJpaRepositories(basePackages = {
    "com.hrconnect.employee",      // Repositories de l'application
    "com.hrconnect.socle.kafka.dlq" // DlqMessageRepository du socle
})
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }

}

