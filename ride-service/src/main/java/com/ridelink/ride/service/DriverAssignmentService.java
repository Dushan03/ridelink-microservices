package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.dto.DriverEligibilityDto;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.exception.DriverReservationException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service orchestrating driver matching, distance-based selection,
 * atomic reservation, and multi-candidate conflict retry.
 */
@Service
public class DriverAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DriverAssignmentService.class);

    private final DriverServiceClient driverServiceClient;
    private final RideRepository rideRepository;

    public DriverAssignmentService(DriverServiceClient driverServiceClient,
                                   RideRepository rideRepository) {
        this.driverServiceClient = driverServiceClient;
        this.rideRepository = rideRepository;
    }

    /**
     * Attempts to find and reserve an eligible driver for the specified ride.
     * Retries subsequent candidates in the proximity list if the top driver experiences a reservation conflict.
     *
     * @param ride the ride requiring driver assignment
     * @return true if driver assignment and reservation succeeded; false otherwise
     */
    public boolean attemptDriverAssignment(Ride ride) {
        List<DriverEligibilityDto> candidates;
        try {
            candidates = driverServiceClient.findEligibleDrivers(
                    ride.getServiceArea(),
                    ride.getRequestedVehicleType(),
                    ride.getPickupLatitude(),
                    ride.getPickupLongitude(),
                    10
            );
        } catch (Exception e) {
            log.warn("Failed to query Driver Service for eligible drivers: {}", e.getMessage());
            return false;
        }

        if (candidates == null || candidates.isEmpty()) {
            log.info("No eligible drivers currently available for ride {}", ride.getId());
            return false;
        }

        for (DriverEligibilityDto driver : candidates) {
            try {
                // Attempt atomic reservation on Driver Service
                driverServiceClient.reserveDriver(driver.driverProfileId(), ride.getId());

                // Reservation succeeded -> update local ride entity
                ride.setDriverProfileId(driver.driverProfileId());
                ride.setDriverAccountId(driver.accountId());
                ride.setVehicleId(driver.activeVehicleId());
                ride.setStatus(RideStatus.ASSIGNED);
                ride.setAssignedAt(Instant.now());

                try {
                    rideRepository.save(ride);
                    log.info("Successfully assigned and reserved driver {} for ride {}", driver.driverProfileId(), ride.getId());
                    return true;
                } catch (Exception dbError) {
                    // Compensation: Release driver if local persistence fails
                    log.error("Failed to persist ride assignment locally; compensating by releasing driver {}", driver.driverProfileId(), dbError);
                    try {
                        driverServiceClient.releaseDriver(driver.driverProfileId(), ride.getId(), "Local assignment rollback");
                    } catch (Exception ignored) {}
                    return false;
                }
            } catch (DriverReservationException e) {
                log.info("Driver {} was unavailable or contested; trying next candidate: {}", driver.driverProfileId(), e.getMessage());
            } catch (Exception e) {
                log.warn("Unexpected error while attempting to reserve driver {}: {}", driver.driverProfileId(), e.getMessage());
            }
        }

        return false;
    }
}
