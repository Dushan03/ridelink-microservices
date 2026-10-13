package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request body for computing and persisting a final fare for a completed ride")
public record FareFinalRequest(

        @NotBlank(message = "Ride ID is required")
        @Schema(description = "UUID of the completed ride", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        String rideId,

        @NotBlank(message = "Passenger ID is required")
        @Schema(description = "UUID of the passenger account", example = "b1ffcd88-8b1a-3de7-aa5c-5aa8ac270b22")
        String passengerId,

        @NotNull(message = "Distance in kilometres is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Distance cannot be negative")
        @Schema(description = "Actual ride distance in kilometres", example = "12.40")
        BigDecimal distanceKm,

        @NotNull(message = "Duration in minutes is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Duration cannot be negative")
        @Schema(description = "Actual ride duration in minutes", example = "35.00")
        BigDecimal durationMinutes
) {}
