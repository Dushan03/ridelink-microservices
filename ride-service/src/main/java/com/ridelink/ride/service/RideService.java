package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareResponseDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.exception.InvalidRideTransitionException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideAccessDeniedException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core business logic and state machine service for Ride Management.
 */
@Service
@Transactional
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final DriverAssignmentService driverAssignmentService;
    private final DriverServiceClient driverServiceClient;
    private final FareServiceClient fareServiceClient;

    public RideService(RideRepository rideRepository,
                       DriverAssignmentService driverAssignmentService,
                       DriverServiceClient driverServiceClient,
                       FareServiceClient fareServiceClient) {
        this.rideRepository = rideRepository;
        this.driverAssignmentService = driverAssignmentService;
        this.driverServiceClient = driverServiceClient;
        this.fareServiceClient = fareServiceClient;
    }

    /**
     * Creates a new ride request, calculates fare estimate, and attempts driver assignment.
     */
    public RideResponse createRide(String passengerAccountId, CreateRideRequest request) {
        request.validate();

        // 1. Calculate fare estimate with Fare Service
        FareResponseDto fareEstimate = fareServiceClient.calculateEstimate(
                request.estimatedDistanceKm(),
                request.estimatedDurationMinutes()
        );

        // 2. Instantiate and persist ride in REQUESTED status
        Ride ride = Ride.builder()
                .id(UUID.randomUUID().toString())
                .passengerAccountId(passengerAccountId)
                .pickupName(request.pickupName().trim())
                .pickupLatitude(request.pickupLatitude())
                .pickupLongitude(request.pickupLongitude())
                .destinationName(request.destinationName().trim())
                .destinationLatitude(request.destinationLatitude())
                .destinationLongitude(request.destinationLongitude())
                .serviceArea(request.serviceArea().trim())
                .requestedVehicleType(request.requestedVehicleType())
                .estimatedDistanceKm(request.estimatedDistanceKm())
                .estimatedDurationMinutes(request.estimatedDurationMinutes())
                .estimatedFareAmount(fareEstimate != null ? fareEstimate.totalAmount() : null)
                .currency(fareEstimate != null && fareEstimate.currency() != null ? fareEstimate.currency() : "LKR")
                .status(RideStatus.REQUESTED)
                .requestedAt(Instant.now())
                .build();

        ride = rideRepository.save(ride);

        // 3. Attempt initial driver discovery and reservation
        driverAssignmentService.attemptDriverAssignment(ride);

        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Retries driver assignment for a ride still in REQUESTED state.
     */
    public RideResponse assignDriver(String rideId, String callerAccountId) {
        Ride ride = findRideOrThrow(rideId);

        if (!ride.getPassengerAccountId().equals(callerAccountId)) {
            throw new RideAccessDeniedException("You are not authorized to retry assignment for this ride");
        }

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideTransitionException(
                    "Driver assignment can only be retried for rides in REQUESTED state (current: " + ride.getStatus() + ")");
        }

        boolean assigned = driverAssignmentService.attemptDriverAssignment(ride);
        if (!assigned) {
            throw new NoAvailableDriverException("No eligible drivers currently available in " + ride.getServiceArea() + ". Please retry shortly.");
        }

        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Retrieves ride details with ownership validation.
     */
    @Transactional(readOnly = true)
    public RideResponse getRideById(String rideId, String callerAccountId, String callerRole) {
        Ride ride = findRideOrThrow(rideId);

        boolean isPassenger = callerAccountId.equals(ride.getPassengerAccountId());
        boolean isDriver = callerAccountId.equals(ride.getDriverAccountId());
        boolean isAdmin = callerRole != null && callerRole.contains("ADMIN");

        if (!isPassenger && !isDriver && !isAdmin) {
            throw new RideAccessDeniedException("You do not have permission to view ride: " + rideId);
        }

        return RideResponse.from(ride);
    }

    /**
     * Retrieves ride history for the authenticated passenger.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getPassengerRides(String passengerAccountId) {
        return rideRepository.findByPassengerAccountIdOrderByCreatedAtDesc(passengerAccountId)
                .stream()
                .map(RideResponse::from)
                .toList();
    }

    /**
     * Retrieves assigned rides for the authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getDriverRides(String driverAccountId) {
        return rideRepository.findByDriverAccountIdOrderByCreatedAtDesc(driverAccountId)
                .stream()
                .map(RideResponse::from)
                .toList();
    }

    /**
     * Driver accepts an assigned ride (ASSIGNED -> ACCEPTED).
     */
    public RideResponse acceptRide(String rideId, String driverAccountId) {
        Ride ride = findRideOrThrow(rideId);

        validateDriverOwnership(ride, driverAccountId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidRideTransitionException(
                    "Cannot accept ride: status must be ASSIGNED (current: " + ride.getStatus() + ")");
        }

        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());

        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Driver starts the trip (ACCEPTED -> IN_PROGRESS), notifying Driver Service.
     */
    public RideResponse startRide(String rideId, String driverAccountId) {
        Ride ride = findRideOrThrow(rideId);

        validateDriverOwnership(ride, driverAccountId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideTransitionException(
                    "Cannot start trip: ride must be in ACCEPTED state (current: " + ride.getStatus() + ")");
        }

        // Notify Driver Service of trip start
        if (ride.getDriverProfileId() != null) {
            driverServiceClient.startTrip(ride.getDriverProfileId(), rideId);
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());

        return RideResponse.from(rideRepository.save(ride));
    }

    /**
     * Driver completes the trip (IN_PROGRESS -> COMPLETED), calculates final fare, and releases driver.
     */
    public RideResponse completeRide(String rideId, String driverAccountId, CompleteRideRequest request, String token) {
        Ride ride = findRideOrThrow(rideId);

        validateDriverOwnership(ride, driverAccountId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideTransitionException(
                    "Cannot complete ride: trip must be IN_PROGRESS (current: " + ride.getStatus() + ")");
        }

        // 1. Call Fare Service to compute and persist binding final fare
        FareResponseDto finalFare = fareServiceClient.createFinalFare(
                ride.getId(),
                ride.getPassengerAccountId(),
                request.actualDistanceKm(),
                request.actualDurationMinutes(),
                token
        );

        // 2. Update local ride entity
        ride.setActualDistanceKm(request.actualDistanceKm());
        ride.setActualDurationMinutes(request.actualDurationMinutes());
        if (finalFare != null) {
            ride.setFinalFareId(finalFare.fareId());
            ride.setFinalFareAmount(finalFare.totalAmount());
            if (finalFare.currency() != null) {
                ride.setCurrency(finalFare.currency());
            }
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());

        ride = rideRepository.save(ride);

        // 3. Release Driver back to AVAILABLE in Driver Service
        if (ride.getDriverProfileId() != null) {
            try {
                driverServiceClient.releaseDriver(ride.getDriverProfileId(), ride.getId(), "Trip completed");
            } catch (Exception e) {
                log.error("Failed to release driver {} after ride completion: {}", ride.getDriverProfileId(), e.getMessage());
            }
        }

        return RideResponse.from(ride);
    }

    /**
     * Cancels a ride (from REQUESTED, ASSIGNED, or ACCEPTED) and releases driver if assigned.
     */
    public RideResponse cancelRide(String rideId, String callerAccountId, String callerRole, String reason) {
        Ride ride = findRideOrThrow(rideId);

        boolean isPassenger = callerAccountId.equals(ride.getPassengerAccountId());
        boolean isDriver = callerAccountId.equals(ride.getDriverAccountId());
        boolean isAdmin = callerRole != null && callerRole.contains("ADMIN");

        if (!isPassenger && !isDriver && !isAdmin) {
            throw new RideAccessDeniedException("You do not have permission to cancel this ride");
        }

        if (ride.getStatus() != RideStatus.REQUESTED &&
            ride.getStatus() != RideStatus.ASSIGNED &&
            ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideTransitionException(
                    "Cannot cancel ride in status: " + ride.getStatus() + " (cancellation only allowed before trip starts)");
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledBy(callerAccountId);
        ride.setCancellationReason(reason != null && !reason.isBlank() ? reason.trim() : "Cancelled by user");
        ride.setCancelledAt(Instant.now());

        ride = rideRepository.save(ride);

        // Release driver if assigned
        if (ride.getDriverProfileId() != null) {
            try {
                driverServiceClient.releaseDriver(ride.getDriverProfileId(), ride.getId(), "Ride cancelled by " + callerAccountId);
            } catch (Exception e) {
                log.warn("Failed to release driver {} during cancellation: {}", ride.getDriverProfileId(), e.getMessage());
            }
        }

        return RideResponse.from(ride);
    }

    /**
     * Admin view of all rides with optional status filter.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getAllRides(RideStatus status) {
        List<Ride> rides = (status != null)
                ? rideRepository.findByStatusOrderByCreatedAtDesc(status)
                : rideRepository.findAllByOrderByCreatedAtDesc();
        return rides.stream().map(RideResponse::from).toList();
    }

    public Ride findRideOrThrow(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    private void validateDriverOwnership(Ride ride, String driverAccountId) {
        if (ride.getDriverAccountId() == null || !ride.getDriverAccountId().equals(driverAccountId)) {
            throw new RideAccessDeniedException("You are not the assigned driver for ride: " + ride.getId());
        }
    }
}
