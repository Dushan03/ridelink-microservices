package com.ridelink.driver.dto;

import com.ridelink.driver.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Year;

/**
 * Request body for PUT /api/drivers/me/vehicles/{vehicleId}
 */
public record UpdateVehicleRequest(
        @Size(min = 1, max = 50, message = "Make must be between 1 and 50 characters")
        String make,

        @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
        String model,

        @Min(value = 1990, message = "Manufacture year must be 1990 or later")
        Integer manufactureYear,

        @Size(min = 1, max = 30, message = "Colour must be between 1 and 30 characters")
        String colour,

        VehicleType vehicleType,

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
