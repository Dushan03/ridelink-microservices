package com.ridelink.driver.dto;

import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.VehicleType;

/**
 * Clean, safe DTO returned for driver discovery by Ride Service or clients.
 * Excludes sensitive personal fields such as driver license numbers.
 */
public record EligibleDriverResponse(
        String driverProfileId,
        String accountId,
        String serviceArea,
        AvailabilityStatus availabilityStatus,
        Double currentLatitude,
        Double currentLongitude,
        Double estimatedDistanceKm,
        String activeVehicleId,
        String vehicleMake,
        String vehicleModel,
        String vehicleColour,
        String vehicleRegistrationNumber,
        VehicleType vehicleType,
        Integer passengerCapacity
) {}
