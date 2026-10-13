package com.ridelink.driver.dto;

import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Safe Driver Profile response DTO returned to API consumers.
 */
public record DriverProfileResponse(
        String id,
        String accountId,
        String licenseNumber,
        LocalDate licenseExpiryDate,
        String serviceArea,
        AvailabilityStatus availabilityStatus,
        Double currentLatitude,
        Double currentLongitude,
        String currentRideId,
        Instant createdAt,
        Instant updatedAt
) {
    public static DriverProfileResponse from(DriverProfile profile) {
        return new DriverProfileResponse(
                profile.getId(),
                profile.getAccountId(),
                profile.getLicenseNumber(),
                profile.getLicenseExpiryDate(),
                profile.getServiceArea(),
                profile.getAvailabilityStatus(),
                profile.getCurrentLatitude(),
                profile.getCurrentLongitude(),
                profile.getCurrentRideId(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
