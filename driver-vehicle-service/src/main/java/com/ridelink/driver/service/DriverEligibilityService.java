package com.ridelink.driver.service;

import com.ridelink.driver.dto.EligibleDriverResponse;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.entity.Vehicle;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for matching and retrieving eligible available drivers.
 * Uses Haversine spherical distance calculation when coordinates are provided.
 */
@Service
@Transactional(readOnly = true)
public class DriverEligibilityService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;

    public DriverEligibilityService(DriverProfileRepository driverProfileRepository,
                                    VehicleRepository vehicleRepository) {
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Searches for eligible available drivers according to business and geospatial criteria.
     *
     * @param serviceArea      service area filter (e.g. "Colombo")
     * @param vehicleType      vehicle type filter (e.g. CAR, THREE_WHEELER)
     * @param pickupLatitude   optional pickup latitude for distance calculation
     * @param pickupLongitude  optional pickup longitude for distance calculation
     * @param limit            maximum number of drivers to return (default 10)
     * @return sorted list of eligible available drivers
     */
    public List<EligibleDriverResponse> findEligibleDrivers(
            String serviceArea,
            VehicleType vehicleType,
            Double pickupLatitude,
            Double pickupLongitude,
            Integer limit) {

        int maxLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        LocalDate today = LocalDate.now();

        // 1. Fetch all drivers currently in AVAILABLE state
        List<DriverProfile> availableDrivers;
        if (serviceArea != null && !serviceArea.isBlank()) {
            availableDrivers = driverProfileRepository.findByAvailabilityStatusAndServiceAreaIgnoreCase(
                    AvailabilityStatus.AVAILABLE, serviceArea.trim());
        } else {
            availableDrivers = driverProfileRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        List<EligibleDriverResponse> results = new ArrayList<>();

        for (DriverProfile driver : availableDrivers) {
            // 2. Filter: License must not be expired
            if (driver.getLicenseExpiryDate() == null || !driver.getLicenseExpiryDate().isAfter(today)) {
                continue;
            }

            // 3. Filter: Must have an active vehicle
            List<Vehicle> activeVehicles = vehicleRepository.findByDriverProfileIdAndActiveTrue(driver.getId());
            if (activeVehicles.isEmpty()) {
                continue;
            }

            // Select matching active vehicle
            Vehicle selectedVehicle = null;
            if (vehicleType != null) {
                selectedVehicle = activeVehicles.stream()
                        .filter(v -> v.getVehicleType() == vehicleType)
                        .findFirst()
                        .orElse(null);
                if (selectedVehicle == null) {
                    continue; // No active vehicle matching the requested type
                }
            } else {
                selectedVehicle = activeVehicles.get(0);
            }

            // 4. Calculate Haversine distance if pickup coordinates provided
            Double distanceKm = null;
            if (pickupLatitude != null && pickupLongitude != null &&
                driver.getCurrentLatitude() != null && driver.getCurrentLongitude() != null) {
                distanceKm = calculateHaversineDistance(
                        pickupLatitude, pickupLongitude,
                        driver.getCurrentLatitude(), driver.getCurrentLongitude()
                );
            }

            results.add(new EligibleDriverResponse(
                    driver.getId(),
                    driver.getAccountId(),
                    driver.getServiceArea(),
                    driver.getAvailabilityStatus(),
                    driver.getCurrentLatitude(),
                    driver.getCurrentLongitude(),
                    distanceKm != null ? Math.round(distanceKm * 100.0) / 100.0 : null,
                    selectedVehicle.getId(),
                    selectedVehicle.getMake(),
                    selectedVehicle.getModel(),
                    selectedVehicle.getColour(),
                    selectedVehicle.getRegistrationNumber(),
                    selectedVehicle.getVehicleType(),
                    selectedVehicle.getPassengerCapacity()
            ));
        }

        // 5. Sort by distance ascending if distance is available
        if (pickupLatitude != null && pickupLongitude != null) {
            results.sort(Comparator.comparing(
                    EligibleDriverResponse::estimatedDistanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        }

        return results.stream().limit(maxLimit).toList();
    }

    /**
     * Haversine formula calculation:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1−a))
     * d = R * c
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0) * Math.cos(radLat1) * Math.cos(radLat2);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }
}
