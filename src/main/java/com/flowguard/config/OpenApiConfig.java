package com.flowguard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flowGuardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlowGuard API")
                        .description("Distributed API Rate Limiting Middleware — "
                                + "Protects APIs from traffic spikes, abuse, and bot attacks "
                                + "with Redis-backed sliding window rate limiting, Kafka event streaming, "
                                + "and real-time monitoring.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FlowGuard Team")
                                .email("admin@flowguard.io"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("API Key"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("API Key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-KEY")
                                        .description("API Key for rate-limited endpoints"))
                        .addSecuritySchemes("Bearer JWT",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from /v1/api/auth/login")));
    }
}
