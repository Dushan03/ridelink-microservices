package com.ridelink.ride.dto;

/**
 * Request DTO sent to Driver Service for driver release.
 */
public record ReleaseDriverDto(
        String rideId,
        String reason
) {}
