package com.ridelink.driver.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for PUT /api/drivers/me
 */
public record UpdateDriverProfileRequest(
        @Size(min = 2, max = 100, message = "Service area must be between 2 and 100 characters")
        String serviceArea,

        @Future(message = "License expiry date must be in the future")
        LocalDate licenseExpiryDate
) {}
