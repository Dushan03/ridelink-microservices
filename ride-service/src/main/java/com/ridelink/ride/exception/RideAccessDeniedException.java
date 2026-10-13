package com.ridelink.ride.exception;

/**
 * Thrown when an authenticated user attempts to access or modify a ride they do not own.
 */
public class RideAccessDeniedException extends RuntimeException {
    public RideAccessDeniedException(String message) {
        super(message);
    }
}
