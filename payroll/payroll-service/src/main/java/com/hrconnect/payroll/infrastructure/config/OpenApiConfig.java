package com.hrconnect.payroll.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                .title("Payroll Service API")
                .description("Microservice de gestion des fiches de paie - HRConnectPro")
                .version("1.0.0")
                .contact(new Contact()
                    .name("HRConnectPro Team")
                    .email("support@hrconnect.com")))
            // Configuration sécurité JWT
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
              .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                  .name("bearerAuth")
                  .type(SecurityScheme.Type.HTTP)
                  .scheme("bearer")
                  .bearerFormat("JWT")));
    }
}

