package com.ridelink.ride.dto;

import java.math.BigDecimal;

/**
 * Interservice DTO sent to Fare Service POST /api/fares/estimate.
 */
public record FareEstimateRequestDto(
        BigDecimal distanceKm,
        BigDecimal durationMinutes
) {}
