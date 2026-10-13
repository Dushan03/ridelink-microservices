package com.ridelink.driver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Driver & Vehicle Service.
 * Defines JWT Bearer scheme and X-Internal-Service-Key header scheme for Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driverOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink – Driver & Vehicle Service API")
                        .description("""
                                **Service 2 (Member 2 · Port 8082)**
                                
                                Manages driver operational profiles, vehicle registration, availability state transitions,
                                eligible driver discovery (Haversine geospatial calculation), and internal ride reservations.
                                
                                ### Security:
                                * **User APIs**: Authenticated via JWT Bearer token (`ROLE_DRIVER` or `ROLE_ADMIN`).
                                * **Internal Interservice APIs**: Protected by `X-Internal-Service-Key` header.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RideLink Engineering Team")
                                .email("dev@ridelink.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token issued by Account Service"))
                        .addSecuritySchemes("internalKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Internal-Service-Key")
                                .description("Internal service secret key for interservice calls from Ride Service")));
    }
}
