package com.hrconnect.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale du microservice Leave-Service
 * Gestion des congés et CRA
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.hrconnect.leave", "com.hrconnect.socle"})
@EnableKafka
@EnableScheduling
public class LeaveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}

