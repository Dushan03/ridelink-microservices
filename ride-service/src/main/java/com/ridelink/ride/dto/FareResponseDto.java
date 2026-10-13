package com.ridelink.ride.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Interservice DTO received from Fare Service.
 */
public record FareResponseDto(
        String fareId,
        String rideId,
        String passengerId,
        BigDecimal distanceKm,
        BigDecimal durationMinutes,
        BigDecimal baseFare,
        BigDecimal ratePerKm,
        BigDecimal ratePerMinute,
        BigDecimal totalAmount,
        String currency,
        String fareType,
        Instant createdAt,
        Instant finalizedAt
) {}
