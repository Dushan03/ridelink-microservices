package com.ridelink.driver.dto;

import jakarta.validation.constraints.*;

/**
 * Request body for PATCH /api/drivers/me/location
 */
public record UpdateLocationRequest(
        @NotBlank(message = "Service area is required")
        @Size(min = 2, max = 100, message = "Service area must be between 2 and 100 characters")
        String serviceArea,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double currentLatitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double currentLongitude
) {}
