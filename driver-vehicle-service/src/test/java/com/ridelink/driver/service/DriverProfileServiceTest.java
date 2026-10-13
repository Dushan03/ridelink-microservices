package com.ridelink.driver.service;

import com.ridelink.driver.dto.CreateDriverProfileRequest;
import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.dto.UpdateLocationRequest;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.exception.DriverProfileAlreadyExistsException;
import com.ridelink.driver.exception.InvalidAvailabilityTransitionException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverProfileService driverProfileService;

    private DriverProfile profile;
    private CreateDriverProfileRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateDriverProfileRequest(
                "LIC-987654",
                LocalDate.now().plusYears(2),
                "Colombo",
                6.9271,
                79.8612
        );

        profile = DriverProfile.builder()
                .id("driver-101")
                .accountId("acc-driver-1")
                .licenseNumber("LIC-987654")
                .licenseExpiryDate(LocalDate.now().plusYears(2))
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .build();
    }

    @Test
    @DisplayName("Successfully create driver profile")
    void createProfile_Success() {
        when(driverProfileRepository.existsByAccountId("acc-driver-1")).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber("LIC-987654")).thenReturn(false);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        DriverProfileResponse response = driverProfileService.createProfile("acc-driver-1", createRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("driver-101");
        assertThat(response.accountId()).isEqualTo("acc-driver-1");
        assertThat(response.licenseNumber()).isEqualTo("LIC-987654");
        assertThat(response.availabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    @DisplayName("Fail creating profile when account already has one")
    void createProfile_DuplicateAccount_ThrowsException() {
        when(driverProfileRepository.existsByAccountId("acc-driver-1")).thenReturn(true);

        assertThatThrownBy(() -> driverProfileService.createProfile("acc-driver-1", createRequest))
                .isInstanceOf(DriverProfileAlreadyExistsException.class)
                .hasMessageContaining("already exists for this account");
    }

    @Test
    @DisplayName("Fail creating profile when license number is already registered")
    void createProfile_DuplicateLicense_ThrowsException() {
        when(driverProfileRepository.existsByAccountId("acc-driver-1")).thenReturn(false);
        when(driverProfileRepository.existsByLicenseNumber("LIC-987654")).thenReturn(true);

        assertThatThrownBy(() -> driverProfileService.createProfile("acc-driver-1", createRequest))
                .isInstanceOf(DriverProfileAlreadyExistsException.class)
                .hasMessageContaining("is already registered");
    }

    @Test
    @DisplayName("Successfully transition from OFFLINE to AVAILABLE when all prerequisites met")
    void updateAvailability_Offline_To_Available_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));
        when(vehicleRepository.countByDriverProfileIdAndActiveTrue("driver-101")).thenReturn(1L);
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE);
        DriverProfileResponse res = driverProfileService.updateAvailability("acc-driver-1", req);

        assertThat(res).isNotNull();
        assertThat(profile.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Successfully transition from AVAILABLE to OFFLINE")
    void updateAvailability_Available_To_Offline_Success() {
        profile.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.OFFLINE);
        DriverProfileResponse res = driverProfileService.updateAvailability("acc-driver-1", req);

        assertThat(res).isNotNull();
        assertThat(profile.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
    }

    @Test
    @DisplayName("Fail transition to AVAILABLE when driver has no active vehicles")
    void updateAvailability_NoActiveVehicle_ThrowsException() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));
        when(vehicleRepository.countByDriverProfileIdAndActiveTrue("driver-101")).thenReturn(0L);

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE);

        assertThatThrownBy(() -> driverProfileService.updateAvailability("acc-driver-1", req))
                .isInstanceOf(InvalidAvailabilityTransitionException.class)
                .hasMessageContaining("without at least one active registered vehicle");
    }

    @Test
    @DisplayName("Fail transition to AVAILABLE when driver license is expired")
    void updateAvailability_ExpiredLicense_ThrowsException() {
        profile.setLicenseExpiryDate(LocalDate.now().minusDays(1));
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE);

        assertThatThrownBy(() -> driverProfileService.updateAvailability("acc-driver-1", req))
                .isInstanceOf(InvalidAvailabilityTransitionException.class)
                .hasMessageContaining("expired or missing driver license");
    }

    @Test
    @DisplayName("Fail transition to AVAILABLE when location coordinates are missing")
    void updateAvailability_MissingLocation_ThrowsException() {
        profile.setCurrentLatitude(null);
        profile.setCurrentLongitude(null);
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));
        when(vehicleRepository.countByDriverProfileIdAndActiveTrue("driver-101")).thenReturn(1L);

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE);

        assertThatThrownBy(() -> driverProfileService.updateAvailability("acc-driver-1", req))
                .isInstanceOf(InvalidAvailabilityTransitionException.class)
                .hasMessageContaining("without setting current simulated location coordinates");
    }

    @Test
    @DisplayName("Fail manual transition when driver is currently in RESERVED or ON_TRIP")
    void updateAvailability_WhileReserved_ThrowsException() {
        profile.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));

        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest(AvailabilityStatus.OFFLINE);

        assertThatThrownBy(() -> driverProfileService.updateAvailability("acc-driver-1", req))
                .isInstanceOf(InvalidAvailabilityTransitionException.class)
                .hasMessageContaining("Cannot manually change availability while driver is in active ride state");
    }

    @Test
    @DisplayName("Successfully update simulated location and service area")
    void updateLocation_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(profile));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(profile);

        UpdateLocationRequest req = new UpdateLocationRequest("Kandy", 7.2906, 80.6337);
        DriverProfileResponse res = driverProfileService.updateLocation("acc-driver-1", req);

        assertThat(res).isNotNull();
        assertThat(profile.getServiceArea()).isEqualTo("Kandy");
        assertThat(profile.getCurrentLatitude()).isEqualTo(7.2906);
        assertThat(profile.getCurrentLongitude()).isEqualTo(80.6337);
    }
}
