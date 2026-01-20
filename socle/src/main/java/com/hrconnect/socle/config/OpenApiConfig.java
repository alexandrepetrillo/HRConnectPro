package com.hrconnect.socle.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI/Swagger commune à tous les microservices.
 * Inclut la configuration de sécurité JWT Bearer.
 * Chaque service peut personnaliser via les properties ou surcharger ce bean.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:HRConnectPro Service}")
    private String applicationName;

    @Value("${spring.application.description:Microservice HRConnectPro}")
    private String applicationDescription;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description(applicationDescription)
                        .version(applicationVersion)
                        .contact(new Contact()
                                .name("HRConnectPro Team")
                                .email("team@hrconnectpro.com")))
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
