package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.FareResponseDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.exception.DriverServiceException;
import com.ridelink.ride.exception.FareServiceException;
import com.ridelink.ride.exception.InvalidRideTransitionException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideAccessDeniedException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverAssignmentService driverAssignmentService;

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private FareServiceClient fareServiceClient;

    @InjectMocks
    private RideService rideService;

    private CreateRideRequest validCreateRequest;
    private Ride baseRide;

    @BeforeEach
    void setUp() {
        validCreateRequest = new CreateRideRequest(
                "Fort Railway Station",
                6.9344,
                79.8500,
                "Galle Face Green",
                6.9271,
                79.8450,
                "COLOMBO",
                VehicleType.CAR,
                new BigDecimal("5.0"),
                new BigDecimal("15")
        );

        baseRide = Ride.builder()
                .id("ride-001")
                .passengerAccountId("pass-acc-1")
                .driverProfileId("drv-prof-1")
                .driverAccountId("drv-acc-1")
                .vehicleId("veh-1")
                .pickupName("Fort Railway Station")
                .pickupLatitude(6.9344)
                .pickupLongitude(79.8500)
                .destinationName("Galle Face Green")
                .destinationLatitude(6.9271)
                .destinationLongitude(79.8450)
                .serviceArea("COLOMBO")
                .requestedVehicleType(VehicleType.CAR)
                .estimatedDistanceKm(new BigDecimal("5.0"))
                .estimatedDurationMinutes(new BigDecimal("15"))
                .estimatedFareAmount(new BigDecimal("750.00"))
                .currency("LKR")
                .status(RideStatus.REQUESTED)
                .requestedAt(Instant.now())
                .build();
    }

    // 1. Valid ride-request creation & 2. Passenger ID taken from JWT & 9. Fare-estimate call
    @Test
    @DisplayName("Should successfully create a ride request with Fare Service estimate")
    void testCreateRideSuccess() {
        FareResponseDto estimateResponse = new FareResponseDto(
                "est-1", "ride-001", "pass-acc-1",
                new BigDecimal("5.0"), new BigDecimal("15"),
                new BigDecimal("100.00"), new BigDecimal("450.00"), new BigDecimal("150.00"),
                new BigDecimal("750.00"), "LKR", "ESTIMATED", Instant.now(), null
        );
        when(fareServiceClient.calculateEstimate(eq(new BigDecimal("5.0")), eq(new BigDecimal("15"))))
                .thenReturn(estimateResponse);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.createRide("pass-acc-1", validCreateRequest);

        assertNotNull(response);
        assertEquals("pass-acc-1", response.passengerAccountId());
        assertEquals("Fort Railway Station", response.pickupName());
        assertEquals("Galle Face Green", response.destinationName());
        assertEquals(new BigDecimal("750.00"), response.estimatedFareAmount());
        assertEquals("LKR", response.currency());

        verify(fareServiceClient).calculateEstimate(eq(new BigDecimal("5.0")), eq(new BigDecimal("15")));
        verify(driverAssignmentService).attemptDriverAssignment(any(Ride.class));
    }

    // 10. Fare-estimate failure handling
    @Test
    @DisplayName("Should propagate FareServiceException when Fare Service is down")
    void testCreateRideFareServiceUnavailable() {
        when(fareServiceClient.calculateEstimate(any(BigDecimal.class), any(BigDecimal.class)))
                .thenThrow(new FareServiceException("Fare service connection refused"));

        assertThrows(FareServiceException.class, () ->
                rideService.createRide("pass-acc-1", validCreateRequest)
        );
        verify(rideRepository, never()).save(any());
    }

    // 13. No available driver handling on assignDriver retry
    @Test
    @DisplayName("Should throw NoAvailableDriverException on assignment retry when no drivers found")
    void testAssignDriverNoDriverAvailable() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(driverAssignmentService.attemptDriverAssignment(baseRide)).thenReturn(false);

        assertThrows(NoAvailableDriverException.class, () ->
                rideService.assignDriver("ride-001", "pass-acc-1")
        );
    }

    // Assignment retry security check
    @Test
    @DisplayName("Should reject assignDriver retry if caller is not the ride passenger")
    void testAssignDriverUnauthorizedPassenger() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(RideAccessDeniedException.class, () ->
                rideService.assignDriver("ride-001", "unauthorized-passenger")
        );
    }

    // 16. Valid REQUESTED -> ASSIGNED retry
    @Test
    @DisplayName("Should successfully assign driver on retry when driver is found")
    void testAssignDriverSuccess() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(driverAssignmentService.attemptDriverAssignment(baseRide)).thenAnswer(inv -> {
            baseRide.setStatus(RideStatus.ASSIGNED);
            baseRide.setDriverAccountId("drv-acc-1");
            return true;
        });
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.assignDriver("ride-001", "pass-acc-1");

        assertEquals(RideStatus.ASSIGNED, response.status());
        assertEquals("drv-acc-1", response.driverAccountId());
    }

    // 17. Valid ASSIGNED -> ACCEPTED
    @Test
    @DisplayName("Assigned driver can accept ride")
    void testDriverAcceptRideSuccess() {
        baseRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.acceptRide("ride-001", "drv-acc-1");

        assertEquals(RideStatus.ACCEPTED, response.status());
        assertNotNull(response.acceptedAt());
    }

    // 24. Passenger cannot accept a ride & 25. Unassigned driver cannot accept
    @Test
    @DisplayName("Unassigned driver or passenger cannot accept ride")
    void testUnassignedDriverCannotAcceptRide() {
        baseRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(RideAccessDeniedException.class, () ->
                rideService.acceptRide("ride-001", "different-driver-acc")
        );
    }

    // 23. Invalid status transition (cannot accept if not ASSIGNED)
    @Test
    @DisplayName("Cannot accept ride if not in ASSIGNED state")
    void testAcceptRideInvalidState() {
        baseRide.setStatus(RideStatus.REQUESTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(InvalidRideTransitionException.class, () ->
                rideService.acceptRide("ride-001", "drv-acc-1")
        );
    }

    // 18. Valid ACCEPTED -> IN_PROGRESS & 28. Driver Service start-trip call
    @Test
    @DisplayName("Assigned driver can start an accepted ride and notifies Driver Service")
    void testDriverStartRideSuccess() {
        baseRide.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.startRide("ride-001", "drv-acc-1");

        assertEquals(RideStatus.IN_PROGRESS, response.status());
        assertNotNull(response.startedAt());
        verify(driverServiceClient).startTrip("drv-prof-1", "ride-001");
    }

    // 29. Unassigned driver cannot start a ride
    @Test
    @DisplayName("Unassigned driver cannot start ride")
    void testUnassignedDriverCannotStartRide() {
        baseRide.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(RideAccessDeniedException.class, () ->
                rideService.startRide("ride-001", "intruder-driver")
        );
        verify(driverServiceClient, never()).startTrip(anyString(), anyString());
    }

    // 19. Valid IN_PROGRESS -> COMPLETED & 30. Final-fare call & 31. Driver release & 33. Final fare stored
    @Test
    @DisplayName("Driver completes trip, calls Fare Service for final fare, and releases driver")
    void testCompleteRideSuccess() {
        baseRide.setStatus(RideStatus.IN_PROGRESS);
        CompleteRideRequest completeReq = new CompleteRideRequest(new BigDecimal("5.5"), new BigDecimal("18"));
        FareResponseDto finalFareDto = new FareResponseDto(
                "fare-final-123", "ride-001", "pass-acc-1",
                new BigDecimal("5.5"), new BigDecimal("18"),
                new BigDecimal("100.00"), new BigDecimal("495.00"), new BigDecimal("180.00"),
                new BigDecimal("825.00"), "LKR", "FINAL", Instant.now(), Instant.now()
        );

        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(fareServiceClient.createFinalFare(eq("ride-001"), eq("pass-acc-1"), eq(new BigDecimal("5.5")), eq(new BigDecimal("18")), any()))
                .thenReturn(finalFareDto);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.completeRide("ride-001", "drv-acc-1", completeReq, "jwt-token");

        assertEquals(RideStatus.COMPLETED, response.status());
        assertEquals(new BigDecimal("5.5"), response.actualDistanceKm());
        assertEquals(new BigDecimal("18"), response.actualDurationMinutes());
        assertEquals("fare-final-123", response.finalFareId());
        assertEquals(new BigDecimal("825.00"), response.finalFareAmount());
        assertNotNull(response.completedAt());

        verify(fareServiceClient).createFinalFare("ride-001", "pass-acc-1", new BigDecimal("5.5"), new BigDecimal("18"), "jwt-token");
        verify(driverServiceClient).releaseDriver("drv-prof-1", "ride-001", "Trip completed");
    }

    // 20. Valid cancellation from REQUESTED
    @Test
    @DisplayName("Passenger can cancel ride in REQUESTED status")
    void testCancelRideFromRequested() {
        baseRide.setStatus(RideStatus.REQUESTED);
        baseRide.setDriverProfileId(null);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide("ride-001", "pass-acc-1", "ROLE_PASSENGER", "Changed my mind");

        assertEquals(RideStatus.CANCELLED, response.status());
        assertEquals("pass-acc-1", response.cancelledBy());
        assertEquals("Changed my mind", response.cancellationReason());
        verify(driverServiceClient, never()).releaseDriver(anyString(), anyString(), anyString());
    }

    // 21. Valid cancellation from ASSIGNED & 32. Driver release after cancellation
    @Test
    @DisplayName("Passenger can cancel ride in ASSIGNED status and releases driver")
    void testCancelRideFromAssignedReleasesDriver() {
        baseRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide("ride-001", "pass-acc-1", "ROLE_PASSENGER", "Found other transport");

        assertEquals(RideStatus.CANCELLED, response.status());
        verify(driverServiceClient).releaseDriver("drv-prof-1", "ride-001", "Ride cancelled by pass-acc-1");
    }

    // 22. Valid cancellation from ACCEPTED
    @Test
    @DisplayName("Passenger can cancel ride in ACCEPTED status")
    void testCancelRideFromAccepted() {
        baseRide.setStatus(RideStatus.ACCEPTED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide("ride-001", "pass-acc-1", "ROLE_PASSENGER", "Emergency");

        assertEquals(RideStatus.CANCELLED, response.status());
        verify(driverServiceClient).releaseDriver("drv-prof-1", "ride-001", "Ride cancelled by pass-acc-1");
    }

    // Cannot cancel IN_PROGRESS or COMPLETED
    @Test
    @DisplayName("Cannot cancel ride that is IN_PROGRESS")
    void testCannotCancelInProgressRide() {
        baseRide.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(InvalidRideTransitionException.class, () ->
                rideService.cancelRide("ride-001", "pass-acc-1", "ROLE_PASSENGER", "Stop ride")
        );
        verify(driverServiceClient, never()).releaseDriver(anyString(), anyString(), anyString());
    }

    // 34. Passenger can view their own ride & 36. Assigned driver can view the ride
    @Test
    @DisplayName("Passenger and assigned driver can view ride details")
    void testGetRideByIdAllowedForPassengerAndDriver() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        RideResponse passView = rideService.getRideById("ride-001", "pass-acc-1", "ROLE_PASSENGER");
        assertNotNull(passView);

        RideResponse drvView = rideService.getRideById("ride-001", "drv-acc-1", "ROLE_DRIVER");
        assertNotNull(drvView);

        RideResponse adminView = rideService.getRideById("ride-001", "admin-acc", "ROLE_ADMIN");
        assertNotNull(adminView);
    }

    // 35. Passenger cannot view another passenger's ride & 37. Unassigned driver cannot view
    @Test
    @DisplayName("Unrelated user cannot view ride details")
    void testGetRideByIdDeniedForUnrelatedUser() {
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));

        assertThrows(RideAccessDeniedException.class, () ->
                rideService.getRideById("ride-001", "stranger-account", "ROLE_PASSENGER")
        );
    }

    // 38. Administrator retrieval
    @Test
    @DisplayName("Admin can view all rides")
    void testAdminGetAllRides() {
        when(rideRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(baseRide));

        List<RideResponse> allRides = rideService.getAllRides(null);
        assertEquals(1, allRides.size());
    }

    // 44. Resource-not-found response
    @Test
    @DisplayName("Throws RideNotFoundException when ride ID does not exist")
    void testRideNotFound() {
        when(rideRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () ->
                rideService.findRideOrThrow("non-existent")
        );
    }

    // Passenger Ride History
    @Test
    @DisplayName("Passenger can retrieve ride history")
    void testGetPassengerRides() {
        when(rideRepository.findByPassengerAccountIdOrderByCreatedAtDesc("pass-acc-1"))
                .thenReturn(List.of(baseRide));

        List<RideResponse> history = rideService.getPassengerRides("pass-acc-1");

        assertEquals(1, history.size());
        assertEquals("ride-001", history.get(0).id());
    }

    // Driver Ride History
    @Test
    @DisplayName("Driver can retrieve assigned rides history")
    void testGetDriverRides() {
        when(rideRepository.findByDriverAccountIdOrderByCreatedAtDesc("drv-acc-1"))
                .thenReturn(List.of(baseRide));

        List<RideResponse> driverRides = rideService.getDriverRides("drv-acc-1");

        assertEquals(1, driverRides.size());
        assertEquals("ride-001", driverRides.get(0).id());
    }

    // Admin Filter by Status
    @Test
    @DisplayName("Admin can filter rides by status")
    void testAdminGetRidesByStatus() {
        when(rideRepository.findByStatusOrderByCreatedAtDesc(RideStatus.REQUESTED))
                .thenReturn(List.of(baseRide));

        List<RideResponse> filteredRides = rideService.getAllRides(RideStatus.REQUESTED);

        assertEquals(1, filteredRides.size());
        assertEquals(RideStatus.REQUESTED, filteredRides.get(0).status());
    }

    // Fault Tolerance: Driver release exception does not fail ride completion
    @Test
    @DisplayName("Driver Service failure during completion driver release is handled gracefully")
    void testCompleteRideDriverReleaseFailureHandledGracefully() {
        baseRide.setStatus(RideStatus.IN_PROGRESS);
        CompleteRideRequest completeReq = new CompleteRideRequest(new BigDecimal("5.5"), new BigDecimal("18"));
        FareResponseDto finalFareDto = new FareResponseDto(
                "fare-final-123", "ride-001", "pass-acc-1",
                new BigDecimal("5.5"), new BigDecimal("18"),
                new BigDecimal("100.00"), new BigDecimal("495.00"), new BigDecimal("180.00"),
                new BigDecimal("825.00"), "LKR", "FINAL", Instant.now(), Instant.now()
        );

        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(fareServiceClient.createFinalFare(any(), any(), any(), any(), any()))
                .thenReturn(finalFareDto);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Driver service network timeout"))
                .when(driverServiceClient).releaseDriver(any(), any(), any());

        RideResponse response = rideService.completeRide("ride-001", "drv-acc-1", completeReq, "jwt-token");

        assertEquals(RideStatus.COMPLETED, response.status());
    }

    // Fault Tolerance: Driver release exception does not fail cancellation
    @Test
    @DisplayName("Driver Service failure during cancellation driver release is handled gracefully")
    void testCancelRideDriverReleaseFailureHandledGracefully() {
        baseRide.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findById("ride-001")).thenReturn(Optional.of(baseRide));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Driver service network timeout"))
                .when(driverServiceClient).releaseDriver(any(), any(), any());

        RideResponse response = rideService.cancelRide("ride-001", "pass-acc-1", "ROLE_PASSENGER", "Cancelled");

        assertEquals(RideStatus.CANCELLED, response.status());
    }
}
