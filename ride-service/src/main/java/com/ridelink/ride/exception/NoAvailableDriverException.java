package com.ridelink.ride.exception;

/**
 * Thrown or returned when no eligible available driver could be found or reserved.
 */
public class NoAvailableDriverException extends RuntimeException {
    public NoAvailableDriverException(String message) {
        super(message);
    }
}
