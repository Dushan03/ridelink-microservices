package com.ridelink.ride.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an interservice REST call to Driver Service or Fare Service fails or times out.
 */
public class InterserviceCommunicationException extends RuntimeException {

    private final HttpStatus httpStatus;

    public InterserviceCommunicationException(String serviceName, String message, HttpStatus httpStatus) {
        super("[" + serviceName + "] " + message);
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.BAD_GATEWAY;
    }

    public InterserviceCommunicationException(String serviceName, String message) {
        this(serviceName, message, HttpStatus.BAD_GATEWAY);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
