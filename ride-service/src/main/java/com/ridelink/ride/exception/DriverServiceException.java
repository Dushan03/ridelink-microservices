package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an interservice call to Driver & Vehicle Service fails.
 */
public class DriverServiceException extends InterserviceCommunicationException {

    public DriverServiceException(String message) {
        super("Driver Service", message, HttpStatus.BAD_GATEWAY);
    }

    public DriverServiceException(String message, HttpStatus status) {
        super("Driver Service", message, status);
    }
}
