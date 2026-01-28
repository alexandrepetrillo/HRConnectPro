package com.hrconnect.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Application principale du microservice Leave-Service
 * Gestion des congés et CRA
 *
 * Note : @ComponentScan inclut les packages du socle technique pour que les beans
 * (JwtAuthenticationFilter, JwtTokenProvider, GlobalExceptionHandler, etc.) soient disponibles.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.hrconnect.leave",         // Package de l'application
    "com.hrconnect.socle"          // Package du socle technique
})
public class LeaveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}

