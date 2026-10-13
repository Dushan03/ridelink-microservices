package com.ridelink.driver.service;

import com.ridelink.driver.dto.RegisterVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.VehicleAlreadyExistsException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private DriverProfile driverProfile;
    private Vehicle vehicle;
    private RegisterVehicleRequest registerRequest;

    @BeforeEach
    void setUp() {
        driverProfile = DriverProfile.builder()
                .id("profile-101")
                .accountId("acc-driver-1")
                .licenseNumber("LIC-12345")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .build();

        vehicle = Vehicle.builder()
                .id("veh-001")
                .driverProfileId("profile-101")
                .registrationNumber("WP-CAR-1234")
                .make("Toyota")
                .model("Prius")
                .manufactureYear(2020)
                .colour("White")
                .vehicleType(VehicleType.CAR)
                .passengerCapacity(4)
                .active(true)
                .build();

        registerRequest = new RegisterVehicleRequest(
                "WP-CAR-1234",
                "Toyota",
                "Prius",
                2020,
                "White",
                VehicleType.CAR,
                4
        );
    }

    @Test
    @DisplayName("Successfully register a vehicle")
    void registerVehicle_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.existsByRegistrationNumber("WP-CAR-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        VehicleResponse response = vehicleService.registerVehicle("acc-driver-1", registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.registrationNumber()).isEqualTo("WP-CAR-1234");
        assertThat(response.driverProfileId()).isEqualTo("profile-101");
        assertThat(response.vehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(response.active()).isTrue();
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Fail vehicle registration when registration number is duplicate")
    void registerVehicle_DuplicateRegNumber_ThrowsException() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.existsByRegistrationNumber("WP-CAR-1234")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.registerVehicle("acc-driver-1", registerRequest))
                .isInstanceOf(VehicleAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Fail vehicle registration when manufacture year is in the future")
    void registerVehicle_FutureYear_ThrowsException() {
        RegisterVehicleRequest futureRequest = new RegisterVehicleRequest(
                "WP-CAR-9999",
                "Toyota",
                "Prius",
                2099,
                "Silver",
                VehicleType.CAR,
                4
        );

        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));

        assertThatThrownBy(() -> vehicleService.registerVehicle("acc-driver-1", futureRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manufacture year cannot be in the future");
    }

    @Test
    @DisplayName("Successfully list driver vehicles")
    void getVehicles_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.findByDriverProfileId("profile-101")).thenReturn(List.of(vehicle));

        List<VehicleResponse> vehicles = vehicleService.getVehicles("acc-driver-1");

        assertThat(vehicles).hasSize(1);
        assertThat(vehicles.get(0).registrationNumber()).isEqualTo("WP-CAR-1234");
    }

    @Test
    @DisplayName("Fail accessing another driver's vehicle")
    void getVehicleById_OtherDriver_ThrowsForbidden() {
        Vehicle otherVehicle = Vehicle.builder()
                .id("veh-other")
                .driverProfileId("different-profile-999")
                .registrationNumber("WP-CAR-5555")
                .build();

        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.findById("veh-other")).thenReturn(Optional.of(otherVehicle));

        assertThatThrownBy(() -> vehicleService.getVehicleById("acc-driver-1", "veh-other"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("do not have permission");
    }

    @Test
    @DisplayName("Successfully update vehicle details")
    void updateVehicle_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.findById("veh-001")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        UpdateVehicleRequest updateReq = new UpdateVehicleRequest(
                "Honda",
                "Civic",
                2021,
                "Black",
                VehicleType.CAR,
                4
        );

        VehicleResponse res = vehicleService.updateVehicle("acc-driver-1", "veh-001", updateReq);

        assertThat(res).isNotNull();
        assertThat(vehicle.getMake()).isEqualTo("Honda");
        assertThat(vehicle.getModel()).isEqualTo("Civic");
        assertThat(vehicle.getColour()).isEqualTo("Black");
    }

    @Test
    @DisplayName("Successfully deactivate vehicle")
    void setVehicleActive_Deactivate_Success() {
        when(driverProfileRepository.findByAccountId("acc-driver-1")).thenReturn(Optional.of(driverProfile));
        when(vehicleRepository.findById("veh-001")).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        VehicleResponse res = vehicleService.setVehicleActive("acc-driver-1", "veh-001", false);

        assertThat(res).isNotNull();
        assertThat(vehicle.isActive()).isFalse();
    }
}
