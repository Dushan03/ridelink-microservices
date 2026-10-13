package com.ridelink.driver.service;

import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.exception.DriverNotAvailableException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.exception.RideMismatchException;
import com.ridelink.driver.repository.DriverProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing internal interservice driver reservation operations for Ride Service.
 */
@Service
@Transactional
public class DriverReservationService {

    private final DriverProfileRepository driverProfileRepository;

    public DriverReservationService(DriverProfileRepository driverProfileRepository) {
        this.driverProfileRepository = driverProfileRepository;
    }

    /**
     * Atomically reserves an available driver for a specific ride.
     * Idempotent: repeated requests for the same driver and same rideId succeed.
     */
    public DriverProfileResponse reserveDriver(String driverId, String rideId) {
        DriverProfile profile = findDriverOrThrow(driverId);

        // Idempotent case: already reserved for this exact ride
        if (profile.getAvailabilityStatus() == AvailabilityStatus.RESERVED &&
            rideId.equals(profile.getCurrentRideId())) {
            return DriverProfileResponse.from(profile);
        }

        if (profile.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new DriverNotAvailableException(
                    "Driver " + driverId + " cannot be reserved: current status is " + profile.getAvailabilityStatus());
        }

        profile.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        profile.setCurrentRideId(rideId);

        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    /**
     * Confirms ride start and transitions driver to ON_TRIP.
     */
    public DriverProfileResponse startTrip(String driverId, String rideId) {
        DriverProfile profile = findDriverOrThrow(driverId);

        if (profile.getAvailabilityStatus() != AvailabilityStatus.RESERVED) {
            throw new RideMismatchException(
                    "Driver " + driverId + " is not in RESERVED state (current: " + profile.getAvailabilityStatus() + ")");
        }

        if (!rideId.equals(profile.getCurrentRideId())) {
            throw new RideMismatchException(
                    "Ride ID mismatch for driver " + driverId + ": expected '" + profile.getCurrentRideId() + "' but got '" + rideId + "'");
        }

        profile.setAvailabilityStatus(AvailabilityStatus.ON_TRIP);
        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    /**
     * Releases driver reservation or completes trip, returning driver to AVAILABLE state.
     */
    public DriverProfileResponse releaseDriver(String driverId, String rideId, String reason) {
        DriverProfile profile = findDriverOrThrow(driverId);

        if (profile.getCurrentRideId() == null || !rideId.equals(profile.getCurrentRideId())) {
            throw new RideMismatchException(
                    "Cannot release driver " + driverId + ": ride ID '" + rideId + "' does not match active ride '" + profile.getCurrentRideId() + "'");
        }

        if (profile.getAvailabilityStatus() != AvailabilityStatus.RESERVED &&
            profile.getAvailabilityStatus() != AvailabilityStatus.ON_TRIP) {
            throw new RideMismatchException(
                    "Driver " + driverId + " is not currently assigned to an active ride (status: " + profile.getAvailabilityStatus() + ")");
        }

        profile.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        profile.setCurrentRideId(null);

        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    private DriverProfile findDriverOrThrow(String driverId) {
        return driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", driverId));
    }
}
