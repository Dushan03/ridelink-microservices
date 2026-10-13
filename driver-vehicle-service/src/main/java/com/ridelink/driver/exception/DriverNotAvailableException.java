package com.ridelink.driver.exception;

/**
 * Thrown when an internal reservation fails because the driver is not currently AVAILABLE.
 */
public class DriverNotAvailableException extends RuntimeException {
    public DriverNotAvailableException(String message) {
        super(message);
    }
}
