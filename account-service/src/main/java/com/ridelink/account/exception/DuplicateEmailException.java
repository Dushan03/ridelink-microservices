package com.ridelink.account.exception;

/** Thrown when an attempted registration email is already registered. */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
