package com.hrconnect.employee.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Employee Service API")
                .description("Microservice de gestion des employés - HRConnectPro")
                .version("1.0.0")
                .contact(new Contact()
                    .name("HRConnectPro Team")
                    .email("support@hrconnect.com")));
    }
}

