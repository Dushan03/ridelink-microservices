package com.ridelink.ride.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for POST /api/rides/{rideId}/complete
 */
public record CompleteRideRequest(
        @NotNull(message = "Actual distance in km is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Actual distance cannot be negative")
        BigDecimal actualDistanceKm,

        @NotNull(message = "Actual duration in minutes is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Actual duration cannot be negative")
        BigDecimal actualDurationMinutes
) {}
