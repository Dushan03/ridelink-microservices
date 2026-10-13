package com.ridelink.ride.exception;

/**
 * Thrown when a requested ride cannot be found in the database.
 */
public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(String rideId) {
        super("Ride not found with identifier: " + rideId);
    }
}
