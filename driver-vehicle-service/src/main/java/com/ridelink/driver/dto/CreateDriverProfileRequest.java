package com.ridelink.driver.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Request body for POST /api/drivers/profile
 * Note: accountId is extracted directly from the authenticated JWT token for security.
 */
public record CreateDriverProfileRequest(
        @NotBlank(message = "License number is required")
        @Size(min = 3, max = 50, message = "License number must be between 3 and 50 characters")
        String licenseNumber,

        @NotNull(message = "License expiry date is required")
        @Future(message = "License expiry date must be in the future")
        LocalDate licenseExpiryDate,

        @NotBlank(message = "Service area is required")
        @Size(min = 2, max = 100, message = "Service area must be between 2 and 100 characters")
        String serviceArea,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        Double currentLatitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        Double currentLongitude
) {}
