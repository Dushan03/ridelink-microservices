package com.ridelink.driver.service;

import com.ridelink.driver.dto.EligibleDriverResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.VehicleType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverEligibilityServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverEligibilityService eligibilityService;

    private DriverProfile driver1;
    private DriverProfile driver2;
    private Vehicle vehicle1;
    private Vehicle vehicle2;

    @BeforeEach
    void setUp() {
        driver1 = DriverProfile.builder()
                .id("driver-colombo-1")
                .accountId("acc-1")
                .licenseNumber("LIC-111")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .currentLatitude(6.9271)
                .currentLongitude(79.8612)
                .build();

        driver2 = DriverProfile.builder()
                .id("driver-colombo-2")
                .accountId("acc-2")
                .licenseNumber("LIC-222")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .currentLatitude(6.9350)
                .currentLongitude(79.8500)
                .build();

        vehicle1 = Vehicle.builder()
                .id("veh-1")
                .driverProfileId("driver-colombo-1")
                .registrationNumber("WP-CAR-1111")
                .make("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .active(true)
                .build();

        vehicle2 = Vehicle.builder()
                .id("veh-2")
                .driverProfileId("driver-colombo-2")
                .registrationNumber("WP-VAN-2222")
                .make("Nissan")
                .model("Caravan")
                .vehicleType(VehicleType.VAN)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Find eligible drivers with service area and vehicle type filter")
    void findEligibleDrivers_FilterServiceAreaAndType() {
        when(driverProfileRepository.findByAvailabilityStatusAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, "Colombo"))
                .thenReturn(List.of(driver1, driver2));
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-1"))
                .thenReturn(List.of(vehicle1));
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-2"))
                .thenReturn(List.of(vehicle2));

        List<EligibleDriverResponse> responses = eligibilityService.findEligibleDrivers(
                "Colombo", VehicleType.CAR, null, null, 10);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).driverProfileId()).isEqualTo("driver-colombo-1");
        assertThat(responses.get(0).vehicleType()).isEqualTo(VehicleType.CAR);
    }

    @Test
    @DisplayName("Exclude drivers with expired licenses")
    void findEligibleDrivers_ExcludeExpiredLicense() {
        driver1.setLicenseExpiryDate(LocalDate.now().minusDays(1)); // Expired

        when(driverProfileRepository.findByAvailabilityStatusAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, "Colombo"))
                .thenReturn(List.of(driver1, driver2));
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-2"))
                .thenReturn(List.of(vehicle2));

        List<EligibleDriverResponse> responses = eligibilityService.findEligibleDrivers(
                "Colombo", null, null, null, 10);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).driverProfileId()).isEqualTo("driver-colombo-2");
    }

    @Test
    @DisplayName("Exclude drivers who have no active vehicles")
    void findEligibleDrivers_ExcludeNoActiveVehicles() {
        when(driverProfileRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(List.of(driver1));
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-1"))
                .thenReturn(List.of()); // No active vehicle

        List<EligibleDriverResponse> responses = eligibilityService.findEligibleDrivers(
                null, null, null, null, 10);

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("Sort eligible drivers by Haversine distance ascending")
    void findEligibleDrivers_SortByDistance() {
        when(driverProfileRepository.findByAvailabilityStatusAndServiceAreaIgnoreCase(AvailabilityStatus.AVAILABLE, "Colombo"))
                .thenReturn(List.of(driver2, driver1)); // Driver 2 is further, Driver 1 is closer
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-1"))
                .thenReturn(List.of(vehicle1));
        when(vehicleRepository.findByDriverProfileIdAndActiveTrue("driver-colombo-2"))
                .thenReturn(List.of(vehicle2));

        // Pickup point near driver 1
        double pickupLat = 6.9270;
        double pickupLon = 79.8610;

        List<EligibleDriverResponse> responses = eligibilityService.findEligibleDrivers(
                "Colombo", null, pickupLat, pickupLon, 10);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).driverProfileId()).isEqualTo("driver-colombo-1");
        assertThat(responses.get(0).estimatedDistanceKm()).isNotNull();
        assertThat(responses.get(0).estimatedDistanceKm()).isLessThan(responses.get(1).estimatedDistanceKm());
    }

    @Test
    @DisplayName("Verify Haversine formula calculation accuracy")
    void calculateHaversineDistance_Accuracy() {
        // Distance between Colombo Fort (6.9344, 79.8428) and Galle Face (6.9234, 79.8428) is ~1.22 km
        double distance = DriverEligibilityService.calculateHaversineDistance(
                6.9344, 79.8428,
                6.9234, 79.8428
        );

        assertThat(distance).isBetween(1.0, 1.5);
    }
}
