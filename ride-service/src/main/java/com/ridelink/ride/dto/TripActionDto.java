package com.ridelink.ride.dto;

/**
 * Request DTO sent to Driver Service for start-trip.
 */
public record TripActionDto(
        String rideId
) {}
