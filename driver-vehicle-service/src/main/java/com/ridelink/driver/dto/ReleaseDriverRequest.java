package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Internal request body for POST /api/internal/drivers/{driverId}/release
 */
public record ReleaseDriverRequest(
        @NotBlank(message = "Ride ID is required")
        String rideId,

        String reason
) {}
