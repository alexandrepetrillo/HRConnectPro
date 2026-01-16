package com.hrconnect.payroll.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI / Swagger
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI payrollServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payroll Service API")
                        .description("API de gestion de la paie - Agrégation multi-sources (Employee, Leave, Interview)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HRConnectPro Team")
                                .email("support@hrconnect.pro"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("Serveur de développement")
                ));
    }
}
