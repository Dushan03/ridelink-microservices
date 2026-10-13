package com.ridelink.driver.entity;

import com.ridelink.driver.enums.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entity representing a Driver's operational profile.
 * Owned exclusively by the Driver & Vehicle Service.
 */
@Entity
@Table(
    name = "driver_profiles",
    indexes = {
        @Index(name = "idx_driver_account_id", columnList = "accountId", unique = true),
        @Index(name = "idx_driver_license_num", columnList = "licenseNumber", unique = true),
        @Index(name = "idx_driver_avail_area", columnList = "availabilityStatus, serviceArea")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfile {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    /** UUID reference to Account Service account (stored as VARCHAR, no JPA foreign entity) */
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String accountId;

    @Column(nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(nullable = false)
    private LocalDate licenseExpiryDate;

    @Column(nullable = false, length = 100)
    private String serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    @Column
    private Double currentLatitude;

    @Column
    private Double currentLongitude;

    /** UUID of the active ride if currently RESERVED or ON_TRIP */
    @Column(length = 36)
    private String currentRideId;

    /** Optimistic locking version for concurrent reservation protection */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (availabilityStatus == null) {
            availabilityStatus = AvailabilityStatus.OFFLINE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
