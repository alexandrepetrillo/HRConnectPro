package com.hrconnect.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payroll Service - Microservice de gestion de la paie
 *
 * Responsabilités :
 * - Gestion des fiches de paie
 * - Calcul des salaires
 *
 * Note : @ComponentScan inclut les packages du socle technique pour que les beans
 * (JwtAuthenticationFilter, JwtTokenProvider, etc.) soient disponibles.
 *
 * Les annotations @EntityScan et @EnableJpaRepositories incluent les packages DLQ
 * du socle pour la gestion des messages en erreur.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hrconnect.payroll",       // Package de l'application
    "com.hrconnect.socle"          // Package du socle technique
})
@EntityScan(basePackages = {
    "com.hrconnect.payroll",       // Entités de l'application
    "com.hrconnect.socle.kafka.dlq" // Entité DlqMessage du socle
})
@EnableJpaRepositories(basePackages = {
    "com.hrconnect.payroll",       // Repositories de l'application
    "com.hrconnect.socle.kafka.dlq" // DlqMessageRepository du socle
})
public class PayrollServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayrollServiceApplication.class, args);
    }

}

