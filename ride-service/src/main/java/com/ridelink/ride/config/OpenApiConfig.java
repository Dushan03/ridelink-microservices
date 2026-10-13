package com.ridelink.ride.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Ride Management Service (Service 3 · Port 8083).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RideLink – Ride Management Service API")
                        .description("""
                                **Service 3 (Member 3 · Port 8083)**
                                
                                Coordinates end-to-end ride lifecycle management, fare estimation integration (Service 4),
                                eligible driver discovery and reservations (Service 2), trip status state transitions,
                                passenger and driver ownership controls, and final fare processing.
                                
                                ### Lifecycle Transitions:
                                * `REQUESTED` → `ASSIGNED` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`
                                * Cancellations permitted from `REQUESTED`, `ASSIGNED`, or `ACCEPTED` states.
                                
                                ### Security:
                                * Authenticated via JWT Bearer token issued by Account Service (Port 8081).
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
                                .description("Enter JWT token issued by Account Service")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
