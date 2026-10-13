package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.dto.DriverEligibilityDto;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.exception.DriverReservationException;
import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverAssignmentServiceTest {

    @Mock
    private DriverServiceClient driverServiceClient;

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private DriverAssignmentService driverAssignmentService;

    private Ride testRide;

    @BeforeEach
    void setUp() {
        testRide = Ride.builder()
                .id("ride-123")
                .passengerAccountId("pass-1")
                .serviceArea("COLOMBO")
                .requestedVehicleType(VehicleType.CAR)
                .pickupLatitude(6.9271)
                .pickupLongitude(79.8612)
                .estimatedDistanceKm(new BigDecimal("5.0"))
                .estimatedDurationMinutes(new BigDecimal("15"))
                .status(RideStatus.REQUESTED)
                .build();
    }

    @Test
    @DisplayName("Should successfully assign and reserve closest eligible driver")
    void testSuccessfulDriverAssignment() {
        DriverEligibilityDto driver1 = new DriverEligibilityDto(
                "drv-prof-1", "drv-acc-1", "COLOMBO", "AVAILABLE",
                6.9280, 79.8620, 1.2,
                "veh-1", "Toyota", "Prius", "White", "WP-CAB-1234",
                VehicleType.CAR, 4
        );
        DriverEligibilityDto driver2 = new DriverEligibilityDto(
                "drv-prof-2", "drv-acc-2", "COLOMBO", "AVAILABLE",
                6.9300, 79.8650, 2.5,
                "veh-2", "Nissan", "Leaf", "Blue", "WP-CAB-5678",
                VehicleType.CAR, 4
        );

        when(driverServiceClient.findEligibleDrivers(eq("COLOMBO"), eq(VehicleType.CAR), eq(6.9271), eq(79.8612), anyInt()))
                .thenReturn(List.of(driver1, driver2));
        doNothing().when(driverServiceClient).reserveDriver(eq("drv-prof-1"), eq("ride-123"));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean assigned = driverAssignmentService.attemptDriverAssignment(testRide);

        assertTrue(assigned);
        assertEquals(RideStatus.ASSIGNED, testRide.getStatus());
        assertEquals("drv-prof-1", testRide.getDriverProfileId());
        assertEquals("drv-acc-1", testRide.getDriverAccountId());
        assertEquals("veh-1", testRide.getVehicleId());
        assertNotNull(testRide.getAssignedAt());

        verify(driverServiceClient).reserveDriver("drv-prof-1", "ride-123");
        verify(driverServiceClient, never()).reserveDriver(eq("drv-prof-2"), anyString());
    }

    @Test
    @DisplayName("Should handle no eligible drivers by remaining in REQUESTED status")
    void testNoEligibleDriversFound() {
        when(driverServiceClient.findEligibleDrivers(anyString(), any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Collections.emptyList());

        boolean assigned = driverAssignmentService.attemptDriverAssignment(testRide);

        assertFalse(assigned);
        assertEquals(RideStatus.REQUESTED, testRide.getStatus());
        assertNull(testRide.getDriverProfileId());
        verify(driverServiceClient, never()).reserveDriver(anyString(), anyString());
    }

    @Test
    @DisplayName("Should retry next candidate if first driver reservation fails and succeed")
    void testReservationConflictFallbackToNextDriver() {
        DriverEligibilityDto driver1 = new DriverEligibilityDto(
                "drv-prof-1", "drv-acc-1", "COLOMBO", "AVAILABLE",
                6.9280, 79.8620, 1.2,
                "veh-1", "Toyota", "Prius", "White", "WP-CAB-1234",
                VehicleType.CAR, 4
        );
        DriverEligibilityDto driver2 = new DriverEligibilityDto(
                "drv-prof-2", "drv-acc-2", "COLOMBO", "AVAILABLE",
                6.9300, 79.8650, 2.5,
                "veh-2", "Nissan", "Leaf", "Blue", "WP-CAB-5678",
                VehicleType.CAR, 4
        );

        when(driverServiceClient.findEligibleDrivers(anyString(), any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(driver1, driver2));
        doThrow(new DriverReservationException("Driver already reserved"))
                .when(driverServiceClient).reserveDriver(eq("drv-prof-1"), eq("ride-123"));
        doNothing().when(driverServiceClient).reserveDriver(eq("drv-prof-2"), eq("ride-123"));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean assigned = driverAssignmentService.attemptDriverAssignment(testRide);

        assertTrue(assigned);
        assertEquals(RideStatus.ASSIGNED, testRide.getStatus());
        assertEquals("drv-prof-2", testRide.getDriverProfileId());
        assertEquals("drv-acc-2", testRide.getDriverAccountId());
        assertEquals("veh-2", testRide.getVehicleId());
    }

    @Test
    @DisplayName("Should compensate by releasing driver if local database persistence fails")
    void testCompensationReleaseWhenDatabaseSaveFails() {
        DriverEligibilityDto driver1 = new DriverEligibilityDto(
                "drv-prof-1", "drv-acc-1", "COLOMBO", "AVAILABLE",
                6.9280, 79.8620, 1.2,
                "veh-1", "Toyota", "Prius", "White", "WP-CAB-1234",
                VehicleType.CAR, 4
        );

        when(driverServiceClient.findEligibleDrivers(anyString(), any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(driver1));
        doNothing().when(driverServiceClient).reserveDriver(eq("drv-prof-1"), eq("ride-123"));
        when(rideRepository.save(any(Ride.class))).thenThrow(new RuntimeException("Database disk failure"));

        boolean assigned = driverAssignmentService.attemptDriverAssignment(testRide);

        assertFalse(assigned);
        verify(driverServiceClient).releaseDriver("drv-prof-1", "ride-123", "Local assignment rollback");
    }
}
