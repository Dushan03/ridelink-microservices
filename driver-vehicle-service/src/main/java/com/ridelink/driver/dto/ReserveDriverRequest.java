package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Internal request body for POST /api/internal/drivers/{driverId}/reserve
 */
public record ReserveDriverRequest(
        @NotBlank(message = "Ride ID is required to reserve a driver")
        String rideId
) {}
