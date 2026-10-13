package com.ridelink.ride.dto;

import com.ridelink.ride.enums.VehicleType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request body for creating a new ride request (POST /api/rides).
 * Note: passengerAccountId is extracted securely from the authenticated JWT token.
 */
public record CreateRideRequest(
        @NotBlank(message = "Pickup place name is required")
        @Size(min = 2, max = 150, message = "Pickup name must be between 2 and 150 characters")
        String pickupName,

        @DecimalMin(value = "-90.0", message = "Pickup latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Pickup latitude must be between -90 and 90")
        Double pickupLatitude,

        @DecimalMin(value = "-180.0", message = "Pickup longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Pickup longitude must be between -180 and 180")
        Double pickupLongitude,

        @NotBlank(message = "Destination place name is required")
        @Size(min = 2, max = 150, message = "Destination name must be between 2 and 150 characters")
        String destinationName,

        @DecimalMin(value = "-90.0", message = "Destination latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Destination latitude must be between -90 and 90")
        Double destinationLatitude,

        @DecimalMin(value = "-180.0", message = "Destination longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Destination longitude must be between -180 and 180")
        Double destinationLongitude,

        @NotBlank(message = "Service area is required")
        @Size(min = 2, max = 100, message = "Service area must be between 2 and 100 characters")
        String serviceArea,

        @NotNull(message = "Requested vehicle type is required")
        VehicleType requestedVehicleType,

        @NotNull(message = "Estimated distance in km is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Estimated distance cannot be negative")
        BigDecimal estimatedDistanceKm,

        @NotNull(message = "Estimated duration in minutes is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Estimated duration cannot be negative")
        BigDecimal estimatedDurationMinutes
) {
    public void validate() {
        if (pickupName != null && destinationName != null &&
            pickupName.trim().equalsIgnoreCase(destinationName.trim())) {
            throw new IllegalArgumentException("Pickup and destination locations cannot be identical");
        }
    }
}
