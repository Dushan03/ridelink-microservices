package com.ridelink.driver.dto;

import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.VehicleType;

import java.time.Instant;

/**
 * Response DTO representing vehicle information.
 */
public record VehicleResponse(
        String id,
        String driverProfileId,
        String registrationNumber,
        String make,
        String model,
        Integer manufactureYear,
        String colour,
        VehicleType vehicleType,
        Integer passengerCapacity,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriverProfileId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getManufactureYear(),
                vehicle.getColour(),
                vehicle.getVehicleType(),
                vehicle.getPassengerCapacity(),
                vehicle.isActive(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
