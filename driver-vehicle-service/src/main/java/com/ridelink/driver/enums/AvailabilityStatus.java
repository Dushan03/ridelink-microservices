package com.ridelink.driver.enums;

/**
 * Lifecycle states of driver availability in RideLink:
 * <ul>
 *   <li>{@code OFFLINE}: Driver is logged off or not accepting ride requests.</li>
 *   <li>{@code AVAILABLE}: Driver is active, ready, and discoverable in their service area.</li>
 *   <li>{@code RESERVED}: Driver is assigned to a ride request pending passenger pickup/start.</li>
 *   <li>{@code ON_TRIP}: Driver has picked up passenger and is currently en route.</li>
 * </ul>
 */
public enum AvailabilityStatus {
    OFFLINE,
    AVAILABLE,
    RESERVED,
    ON_TRIP;

    /**
     * Checks if manual transition requested by the driver is legally permitted.
     * Manual transitions are only allowed between OFFLINE and AVAILABLE.
     */
    public boolean canTransitionManuallyTo(AvailabilityStatus target) {
        if (this == OFFLINE && target == AVAILABLE) return true;
        if (this == AVAILABLE && target == OFFLINE) return true;
        return this == target;
    }
}
