package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an interservice call to Fare & Payment Service fails.
 */
public class FareServiceException extends InterserviceCommunicationException {

    public FareServiceException(String message) {
        super("Fare Service", message, HttpStatus.BAD_GATEWAY);
    }

    public FareServiceException(String message, HttpStatus status) {
        super("Fare Service", message, status);
    }
}
