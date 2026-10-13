package com.ridelink.driver.exception;

/**
 * Thrown when a vehicle registration number is already registered in the system.
 */
public class VehicleAlreadyExistsException extends RuntimeException {
    public VehicleAlreadyExistsException(String registrationNumber) {
        super("Vehicle with registration number '" + registrationNumber + "' already exists");
    }
}
