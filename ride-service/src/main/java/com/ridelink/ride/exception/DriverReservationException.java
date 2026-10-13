package com.ridelink.ride.exception;

/**
 * Thrown when driver reservation fails or experiences conflicts.
 */
public class DriverReservationException extends RuntimeException {
    public DriverReservationException(String message) {
        super(message);
    }
}
