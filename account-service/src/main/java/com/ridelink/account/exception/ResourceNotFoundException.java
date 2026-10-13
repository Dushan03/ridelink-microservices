package com.ridelink.account.exception;

/** Thrown when a requested resource (account, etc.) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String id) {
        super(resourceName + " not found with id: " + id);
    }
}
