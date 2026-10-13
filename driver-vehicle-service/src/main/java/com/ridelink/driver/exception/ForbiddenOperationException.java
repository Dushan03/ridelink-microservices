package com.ridelink.driver.exception;

/**
 * Thrown when an authenticated user attempts an operation on a resource they do not own.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
