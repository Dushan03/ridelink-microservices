package com.ridelink.ride.dto;

/**
 * Request DTO sent to Driver Service for reserving a driver.
 */
public record ReserveDriverDto(
        String rideId
) {}
