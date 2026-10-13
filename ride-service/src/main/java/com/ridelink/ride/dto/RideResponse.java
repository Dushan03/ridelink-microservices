package com.ridelink.ride.dto;

import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Standard safe Ride Response DTO returned to clients and services.
 */
public record RideResponse(
        String id,
        String passengerAccountId,
        String driverProfileId,
        String driverAccountId,
        String vehicleId,
        String pickupName,
        Double pickupLatitude,
        Double pickupLongitude,
        String destinationName,
        Double destinationLatitude,
        Double destinationLongitude,
        String serviceArea,
        VehicleType requestedVehicleType,
        BigDecimal estimatedDistanceKm,
        BigDecimal estimatedDurationMinutes,
        BigDecimal actualDistanceKm,
        BigDecimal actualDurationMinutes,
        BigDecimal estimatedFareAmount,
        String finalFareId,
        BigDecimal finalFareAmount,
        String currency,
        RideStatus status,
        String cancellationReason,
        String cancelledBy,
        Instant requestedAt,
        Instant assignedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getPassengerAccountId(),
                ride.getDriverProfileId(),
                ride.getDriverAccountId(),
                ride.getVehicleId(),
                ride.getPickupName(),
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getDestinationName(),
                ride.getDestinationLatitude(),
                ride.getDestinationLongitude(),
                ride.getServiceArea(),
                ride.getRequestedVehicleType(),
                ride.getEstimatedDistanceKm(),
                ride.getEstimatedDurationMinutes(),
                ride.getActualDistanceKm(),
                ride.getActualDurationMinutes(),
                ride.getEstimatedFareAmount(),
                ride.getFinalFareId(),
                ride.getFinalFareAmount(),
                ride.getCurrency(),
                ride.getStatus(),
                ride.getCancellationReason(),
                ride.getCancelledBy(),
                ride.getRequestedAt(),
                ride.getAssignedAt(),
                ride.getAcceptedAt(),
                ride.getStartedAt(),
                ride.getCompletedAt(),
                ride.getCancelledAt(),
                ride.getCreatedAt(),
                ride.getUpdatedAt()
        );
    }
}
