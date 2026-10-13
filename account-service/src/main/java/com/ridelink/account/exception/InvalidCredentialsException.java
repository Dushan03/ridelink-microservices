package com.ridelink.account.exception;

/** Thrown when the email/password combination does not match any account. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
