package com.ridelink.driver.exception;

/**
 * Thrown when a requested resource (driver profile, vehicle, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found with identifier: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
