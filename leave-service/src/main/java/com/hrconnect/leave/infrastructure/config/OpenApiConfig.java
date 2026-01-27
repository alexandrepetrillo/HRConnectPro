package com.hrconnect.leave.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI/Swagger
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI leaveServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leave Service API")
                        .description("API de gestion des congés - HRConnectPro")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HRConnectPro Team")
                                .email("contact@hrconnect.com")));
    }
}

