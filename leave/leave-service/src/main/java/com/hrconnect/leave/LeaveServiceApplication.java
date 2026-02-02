package com.hrconnect.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application principale du microservice Leave-Service
 * Gestion des congés et CRA
 *
 * Note : @ComponentScan inclut les packages du socle technique pour que les beans
 * (JwtAuthenticationFilter, JwtTokenProvider, GlobalExceptionHandler, etc.) soient disponibles.
 *
 * Les annotations @EntityScan et @EnableJpaRepositories incluent les packages DLQ
 * du socle pour la gestion des messages Kafka en erreur.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hrconnect.leave",         // Package de l'application
    "com.hrconnect.socle"          // Package du socle technique
})
@EntityScan(basePackages = {
    "com.hrconnect.leave",         // Entités de l'application
    "com.hrconnect.socle.kafka.dlq" // Entité DlqMessage du socle
})
@EnableJpaRepositories(basePackages = {
    "com.hrconnect.leave",         // Repositories de l'application
    "com.hrconnect.socle.kafka.dlq" // DlqMessageRepository du socle
})
public class LeaveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}

