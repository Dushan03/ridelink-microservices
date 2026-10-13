package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Internal request body for POST /api/internal/drivers/{driverId}/start-trip
 */
public record TripActionRequest(
        @NotBlank(message = "Ride ID is required")
        String rideId
) {}
