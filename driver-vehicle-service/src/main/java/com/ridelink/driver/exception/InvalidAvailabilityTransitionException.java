package com.ridelink.driver.exception;

/**
 * Thrown when an illegal availability state transition is attempted.
 */
public class InvalidAvailabilityTransitionException extends RuntimeException {
    public InvalidAvailabilityTransitionException(String message) {
        super(message);
    }
}
