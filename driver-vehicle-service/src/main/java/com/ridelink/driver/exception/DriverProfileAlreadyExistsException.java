package com.ridelink.driver.exception;

/**
 * Thrown when attempting to create a driver profile when one already exists for the account or license number.
 */
public class DriverProfileAlreadyExistsException extends RuntimeException {
    public DriverProfileAlreadyExistsException(String message) {
        super(message);
    }
}
