package com.ridelink.fare.exception;

public class DuplicateFinalFareException extends RuntimeException {
    public DuplicateFinalFareException(String rideId) {
        super("A final fare has already been calculated and recorded for ride: " + rideId);
    }
}
