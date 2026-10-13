package com.ridelink.driver.service;

import com.ridelink.driver.dto.RegisterVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.exception.VehicleAlreadyExistsException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          DriverProfileRepository driverProfileRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverProfileRepository = driverProfileRepository;
    }

    /**
     * Registers a new vehicle under the authenticated driver's profile.
     */
    public VehicleResponse registerVehicle(String accountId, RegisterVehicleRequest request) {
        DriverProfile profile = findProfileByAccountId(accountId);
        request.validateYear();

        String regNum = request.registrationNumber().trim().toUpperCase();
        if (vehicleRepository.existsByRegistrationNumber(regNum)) {
            throw new VehicleAlreadyExistsException(regNum);
        }

        Vehicle vehicle = Vehicle.builder()
                .id(UUID.randomUUID().toString())
                .driverProfileId(profile.getId())
                .registrationNumber(regNum)
                .make(request.make().trim())
                .model(request.model().trim())
                .manufactureYear(request.manufactureYear())
                .colour(request.colour().trim())
                .vehicleType(request.vehicleType())
                .passengerCapacity(request.passengerCapacity())
                .active(true)
                .build();

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /**
     * Retrieves all vehicles registered to the authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles(String accountId) {
        DriverProfile profile = findProfileByAccountId(accountId);
        return vehicleRepository.findByDriverProfileId(profile.getId())
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    /**
     * Retrieves a single owned vehicle.
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(String accountId, String vehicleId) {
        DriverProfile profile = findProfileByAccountId(accountId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        validateOwnership(profile, vehicle);
        return VehicleResponse.from(vehicle);
    }

    /**
     * Updates an owned vehicle's details.
     */
    public VehicleResponse updateVehicle(String accountId, String vehicleId, UpdateVehicleRequest request) {
        DriverProfile profile = findProfileByAccountId(accountId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        validateOwnership(profile, vehicle);
        request.validateYear();

        if (request.make() != null && !request.make().isBlank()) {
            vehicle.setMake(request.make().trim());
        }
        if (request.model() != null && !request.model().isBlank()) {
            vehicle.setModel(request.model().trim());
        }
        if (request.manufactureYear() != null) {
            vehicle.setManufactureYear(request.manufactureYear());
        }
        if (request.colour() != null && !request.colour().isBlank()) {
            vehicle.setColour(request.colour().trim());
        }
        if (request.vehicleType() != null) {
            vehicle.setVehicleType(request.vehicleType());
        }
        if (request.passengerCapacity() != null) {
            vehicle.setPassengerCapacity(request.passengerCapacity());
        }

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /**
     * Activates or deactivates an owned vehicle.
     */
    public VehicleResponse setVehicleActive(String accountId, String vehicleId, boolean active) {
        DriverProfile profile = findProfileByAccountId(accountId);
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        validateOwnership(profile, vehicle);
        vehicle.setActive(active);

        // If driver has no more active vehicles and is currently AVAILABLE, automatically transition to OFFLINE
        if (!active && profile.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
            long remainingActive = vehicleRepository.countByDriverProfileIdAndActiveTrue(profile.getId()) - 1;
            if (remainingActive <= 0) {
                profile.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
                driverProfileRepository.save(profile);
            }
        }

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    private DriverProfile findProfileByAccountId(String accountId) {
        return driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile for account " + accountId + " not found"));
    }

    private Vehicle findVehicleOrThrow(String vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
    }

    private void validateOwnership(DriverProfile profile, Vehicle vehicle) {
        if (!vehicle.getDriverProfileId().equals(profile.getId())) {
            throw new ForbiddenOperationException("You do not have permission to access or modify this vehicle");
        }
    }
}
