package com.ridelink.driver.service;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.exception.DriverProfileAlreadyExistsException;
import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.exception.InvalidAvailabilityTransitionException;
import com.ridelink.driver.exception.ResourceNotFoundException;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service managing Driver operational profiles and availability lifecycle.
 */
@Service
@Transactional
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;

    public DriverProfileService(DriverProfileRepository driverProfileRepository,
                                VehicleRepository vehicleRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Creates an operational driver profile for the authenticated account.
     */
    public DriverProfileResponse createProfile(String accountId, CreateDriverProfileRequest request) {
        if (driverProfileRepository.existsByAccountId(accountId)) {
            throw new DriverProfileAlreadyExistsException("A driver profile already exists for this account");
        }

        if (driverProfileRepository.existsByLicenseNumber(request.licenseNumber().trim())) {
            throw new DriverProfileAlreadyExistsException("License number '" + request.licenseNumber() + "' is already registered");
        }

        if (request.licenseExpiryDate().isBefore(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("License expiry date must be in the future");
        }

        DriverProfile profile = DriverProfile.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .licenseNumber(request.licenseNumber().trim().toUpperCase())
                .licenseExpiryDate(request.licenseExpiryDate())
                .serviceArea(request.serviceArea().trim())
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .currentLatitude(request.currentLatitude())
                .currentLongitude(request.currentLongitude())
                .build();

        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    /**
     * Retrieves the profile belonging to the authenticated account.
     */
    @Transactional(readOnly = true)
    public DriverProfileResponse getProfileByAccountId(String accountId) {
        DriverProfile profile = findByAccountIdOrThrow(accountId);
        return DriverProfileResponse.from(profile);
    }

    /**
     * Retrieves a driver profile by its ID (Admin or internal use).
     */
    @Transactional(readOnly = true)
    public DriverProfileResponse getProfileById(String driverId) {
        DriverProfile profile = driverProfileRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile", driverId));
        return DriverProfileResponse.from(profile);
    }

    /**
     * Updates allowed operational profile fields.
     */
    public DriverProfileResponse updateProfile(String accountId, UpdateDriverProfileRequest request) {
        DriverProfile profile = findByAccountIdOrThrow(accountId);

        if (request.serviceArea() != null && !request.serviceArea().isBlank()) {
            profile.setServiceArea(request.serviceArea().trim());
        }

        if (request.licenseExpiryDate() != null) {
            if (request.licenseExpiryDate().isBefore(LocalDate.now().plusDays(1))) {
                throw new IllegalArgumentException("License expiry date must be in the future");
            }
            profile.setLicenseExpiryDate(request.licenseExpiryDate());
        }

        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    /**
     * Updates driver availability status enforcing strict business and state transition rules.
     */
    public DriverProfileResponse updateAvailability(String accountId, UpdateAvailabilityRequest request) {
        DriverProfile profile = findByAccountIdOrThrow(accountId);
        AvailabilityStatus currentStatus = profile.getAvailabilityStatus();
        AvailabilityStatus targetStatus = request.availabilityStatus();

        if (currentStatus == targetStatus) {
            return DriverProfileResponse.from(profile);
        }

        // Driver cannot manually bypass active ride states
        if (currentStatus == AvailabilityStatus.RESERVED || currentStatus == AvailabilityStatus.ON_TRIP) {
            throw new InvalidAvailabilityTransitionException(
                    "Cannot manually change availability while driver is in active ride state: " + currentStatus);
        }

        if (!currentStatus.canTransitionManuallyTo(targetStatus)) {
            throw new InvalidAvailabilityTransitionException(
                    "Invalid manual availability transition from " + currentStatus + " to " + targetStatus);
        }

        // Prerequisites for becoming AVAILABLE:
        if (targetStatus == AvailabilityStatus.AVAILABLE) {
            // 1. License must not be expired
            if (profile.getLicenseExpiryDate() == null || !profile.getLicenseExpiryDate().isAfter(LocalDate.now())) {
                throw new InvalidAvailabilityTransitionException("Cannot become AVAILABLE with an expired or missing driver license");
            }

            // 2. Must have at least one active vehicle
            long activeVehicles = vehicleRepository.countByDriverProfileIdAndActiveTrue(profile.getId());
            if (activeVehicles == 0) {
                throw new InvalidAvailabilityTransitionException("Cannot become AVAILABLE without at least one active registered vehicle");
            }

            // 3. Must have valid service area
            if (profile.getServiceArea() == null || profile.getServiceArea().isBlank()) {
                throw new InvalidAvailabilityTransitionException("Cannot become AVAILABLE without a designated service area");
            }

            // 4. Must have valid simulated coordinates
            if (profile.getCurrentLatitude() == null || profile.getCurrentLongitude() == null) {
                throw new InvalidAvailabilityTransitionException("Cannot become AVAILABLE without setting current simulated location coordinates");
            }

            if (profile.getCurrentLatitude() < -90.0 || profile.getCurrentLatitude() > 90.0 ||
                profile.getCurrentLongitude() < -180.0 || profile.getCurrentLongitude() > 180.0) {
                throw new InvalidAvailabilityTransitionException("Cannot become AVAILABLE with invalid coordinates (Lat [-90,90], Lon [-180,180])");
            }
        }

        profile.setAvailabilityStatus(targetStatus);
        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    /**
     * Updates simulated coordinates and service area.
     */
    public DriverProfileResponse updateLocation(String accountId, UpdateLocationRequest request) {
        DriverProfile profile = findByAccountIdOrThrow(accountId);

        profile.setServiceArea(request.serviceArea().trim());
        profile.setCurrentLatitude(request.currentLatitude());
        profile.setCurrentLongitude(request.currentLongitude());

        return DriverProfileResponse.from(driverProfileRepository.save(profile));
    }

    public DriverProfile findByAccountIdOrThrow(String accountId) {
        return driverProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile for account " + accountId + " not found"));
    }
}
