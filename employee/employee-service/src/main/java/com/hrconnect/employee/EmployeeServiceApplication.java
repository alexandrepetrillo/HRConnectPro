package com.hrconnect.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Employee Service - Microservice de gestion des employés
 *
 * Responsabilités :
 * - Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel (base)
 * - Publication des snapshots Employee sur le topic employee.state
 *
 * Note : @ComponentScan inclut les packages du socle technique pour que les beans
 * (JwtAuthenticationFilter, JwtTokenProvider, etc.) soient disponibles.
 */

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hrconnect.employee",      // Package de l'application
    "com.hrconnect.socle"          // Package du socle technique
})
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }

}

