package com.ridelink.ride.dto;

import java.math.BigDecimal;

/**
 * Interservice DTO sent to Fare Service POST /api/fares/final.
 */
public record FareFinalRequestDto(
        String rideId,
        String passengerId,
        BigDecimal distanceKm,
        BigDecimal durationMinutes
) {}
