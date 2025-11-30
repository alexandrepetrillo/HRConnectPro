package com.hrconnect.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Employee Service - Microservice de gestion des employés
 *
 * Responsabilités :
 * - Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel (base)
 * - Publication des snapshots Employee sur le topic employee.state
 */
@SpringBootApplication
@EnableKafka
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }

}

