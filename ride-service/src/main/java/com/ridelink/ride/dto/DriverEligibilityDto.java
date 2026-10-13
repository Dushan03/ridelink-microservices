package com.ridelink.ride.dto;

import com.ridelink.ride.enums.VehicleType;

/**
 * Interservice DTO matching GET /api/drivers/eligible from Driver Service.
 */
public record DriverEligibilityDto(
        String driverProfileId,
        String accountId,
        String serviceArea,
        String availabilityStatus,
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
