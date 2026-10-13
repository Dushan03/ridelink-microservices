package com.ridelink.account.model;

/**
 * User roles in RideLink.
 * Stored in the JWT claim "role" and on the Account entity.
 */
public enum Role {
    PASSENGER,
    DRIVER,
    ADMIN
}
