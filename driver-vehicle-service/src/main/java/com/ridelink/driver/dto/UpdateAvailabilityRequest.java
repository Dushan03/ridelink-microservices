package com.ridelink.driver.dto;

import com.ridelink.driver.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PATCH /api/drivers/me/availability
 */
public record UpdateAvailabilityRequest(
        @NotNull(message = "Target availability status is required")
        AvailabilityStatus availabilityStatus
) {}
