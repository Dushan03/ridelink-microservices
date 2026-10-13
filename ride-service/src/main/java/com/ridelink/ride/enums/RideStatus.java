package com.ridelink.ride.enums;

/**
 * Lifecycle states of a ride in RideLink:
 * <ul>
 *   <li>{@code REQUESTED}: Passenger created request; driver search in progress or waiting for retry.</li>
 *   <li>{@code ASSIGNED}: Driver has been selected and reserved; awaiting driver acceptance.</li>
 *   <li>{@code ACCEPTED}: Driver accepted the ride; en route to passenger pickup location.</li>
 *   <li>{@code IN_PROGRESS}: Driver picked up passenger; trip is active.</li>
 *   <li>{@code COMPLETED}: Ride completed successfully; final fare generated.</li>
 *   <li>{@code CANCELLED}: Ride was cancelled prior to trip commencement.</li>
 * </ul>
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /**
     * Checks whether a status transition from this status to target is legally allowed.
     */
    public boolean canTransitionTo(RideStatus target) {
        return switch (this) {
            case REQUESTED -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == ACCEPTED || target == CANCELLED;
            case ACCEPTED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false; // Terminal states
        };
    }
}
