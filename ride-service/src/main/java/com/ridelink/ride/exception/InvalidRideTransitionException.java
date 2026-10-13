package com.ridelink.ride.exception;

/**
 * Thrown when an invalid or illegal ride status transition is attempted.
 */
public class InvalidRideTransitionException extends RuntimeException {
    public InvalidRideTransitionException(String message) {
        super(message);
    }
}
