package com.ridelink.driver.service;

import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.exception.DriverNotAvailableException;
import com.ridelink.driver.exception.RideMismatchException;
import com.ridelink.driver.repository.DriverProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverReservationServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @InjectMocks
    private DriverReservationService reservationService;

    private DriverProfile driver;

    @BeforeEach
    void setUp() {
        driver = DriverProfile.builder()
                .id("driver-1")
                .accountId("acc-driver-1")
                .licenseNumber("LIC-123")
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("Successfully reserve available driver")
    void reserveDriver_Success() {
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(driver);

        DriverProfileResponse response = reservationService.reserveDriver("driver-1", "ride-999");

        assertThat(response).isNotNull();
        assertThat(driver.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.RESERVED);
        assertThat(driver.getCurrentRideId()).isEqualTo("ride-999");
        verify(driverProfileRepository).save(driver);
    }

    @Test
    @DisplayName("Fail reservation when driver is not in AVAILABLE state")
    void reserveDriver_NotAvailable_ThrowsException() {
        driver.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> reservationService.reserveDriver("driver-1", "ride-999"))
                .isInstanceOf(DriverNotAvailableException.class)
                .hasMessageContaining("cannot be reserved: current status is OFFLINE");
    }

    @Test
    @DisplayName("Idempotent repeated reservation for the same ride succeeds safely")
    void reserveDriver_SameRide_IdempotentSuccess() {
        driver.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        driver.setCurrentRideId("ride-999");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        DriverProfileResponse response = reservationService.reserveDriver("driver-1", "ride-999");

        assertThat(response).isNotNull();
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.RESERVED);
    }

    @Test
    @DisplayName("Conflict when second ride tries to reserve already reserved driver")
    void reserveDriver_AlreadyReservedForOtherRide_ThrowsConflict() {
        driver.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        driver.setCurrentRideId("ride-first");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> reservationService.reserveDriver("driver-1", "ride-second"))
                .isInstanceOf(DriverNotAvailableException.class)
                .hasMessageContaining("current status is RESERVED");
    }

    @Test
    @DisplayName("Successfully start trip for reserved driver with matching ride ID")
    void startTrip_Success() {
        driver.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        driver.setCurrentRideId("ride-999");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(driver);

        DriverProfileResponse response = reservationService.startTrip("driver-1", "ride-999");

        assertThat(response).isNotNull();
        assertThat(driver.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ON_TRIP);
    }

    @Test
    @DisplayName("Fail start trip when ride ID does not match")
    void startTrip_RideMismatch_ThrowsException() {
        driver.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        driver.setCurrentRideId("ride-999");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> reservationService.startTrip("driver-1", "wrong-ride"))
                .isInstanceOf(RideMismatchException.class)
                .hasMessageContaining("Ride ID mismatch");
    }

    @Test
    @DisplayName("Successfully release driver back to AVAILABLE on cancellation or completion")
    void releaseDriver_Success() {
        driver.setAvailabilityStatus(AvailabilityStatus.ON_TRIP);
        driver.setCurrentRideId("ride-999");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(driver);

        DriverProfileResponse response = reservationService.releaseDriver("driver-1", "ride-999", "Completed successfully");

        assertThat(response).isNotNull();
        assertThat(driver.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(driver.getCurrentRideId()).isNull();
    }

    @Test
    @DisplayName("Fail release driver when ride ID does not match")
    void releaseDriver_RideMismatch_ThrowsException() {
        driver.setAvailabilityStatus(AvailabilityStatus.ON_TRIP);
        driver.setCurrentRideId("ride-999");
        when(driverProfileRepository.findById("driver-1")).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> reservationService.releaseDriver("driver-1", "wrong-ride", "Cancelled"))
                .isInstanceOf(RideMismatchException.class)
                .hasMessageContaining("does not match active ride");
    }
}
