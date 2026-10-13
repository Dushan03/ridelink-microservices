package com.ridelink.fare.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request body for calculating an indicative fare estimate")
public record FareEstimateRequest(

        @NotNull(message = "Distance in kilometres is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Distance cannot be negative")
        @Schema(description = "Estimated ride distance in kilometres", example = "8.50")
        BigDecimal distanceKm,

        @NotNull(message = "Duration in minutes is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Duration cannot be negative")
        @Schema(description = "Estimated ride duration in minutes", example = "22.00")
        BigDecimal durationMinutes
) {}
