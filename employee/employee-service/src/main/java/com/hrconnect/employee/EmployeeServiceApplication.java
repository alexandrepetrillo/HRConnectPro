package com.hrconnect.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Employee Service - Microservice de gestion des employés
 *
 * Responsabilités :
 * - Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel (base)
 * - Publication des snapshots Employee sur le topic employee.state
 * - Pattern Outbox pour garantir la cohérence transactionnelle
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.hrconnect.employee", "com.hrconnect.socle"})
@EnableKafka
@EnableScheduling
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }

}

