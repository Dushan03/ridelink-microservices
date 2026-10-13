package com.ridelink.driver.exception;

/**
 * Thrown when an internal ride action (start-trip, release) specifies a rideId that does not match the driver's current active ride.
 */
public class RideMismatchException extends RuntimeException {
    public RideMismatchException(String message) {
        super(message);
    }
}
