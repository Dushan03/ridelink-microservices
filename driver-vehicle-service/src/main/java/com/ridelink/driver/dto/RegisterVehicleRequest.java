package com.ridelink.driver.dto;

import com.ridelink.driver.enums.VehicleType;
import jakarta.validation.constraints.*;

import java.time.Year;

/**
 * Request body for POST /api/drivers/me/vehicles
 */
public record RegisterVehicleRequest(
        @NotBlank(message = "Registration number is required")
        @Size(min = 2, max = 30, message = "Registration number must be between 2 and 30 characters")
        String registrationNumber,

        @NotBlank(message = "Make is required")
        @Size(min = 1, max = 50, message = "Make must be between 1 and 50 characters")
        String make,

        @NotBlank(message = "Model is required")
        @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
        String model,

        @NotNull(message = "Manufacture year is required")
        @Min(value = 1990, message = "Manufacture year must be 1990 or later")
        Integer manufactureYear,

        @NotBlank(message = "Colour is required")
        @Size(min = 1, max = 30, message = "Colour must be between 1 and 30 characters")
        String colour,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotNull(message = "Passenger capacity is required")
        @Min(value = 1, message = "Passenger capacity must be at least 1")
        @Max(value = 50, message = "Passenger capacity cannot exceed 50")
        Integer passengerCapacity
) {
    public void validateYear() {
        int currentYear = Year.now().getValue();
        if (manufactureYear != null && manufactureYear > currentYear) {
            throw new IllegalArgumentException("Manufacture year cannot be in the future");
        }
    }
}
