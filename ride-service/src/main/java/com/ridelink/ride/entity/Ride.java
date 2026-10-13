package com.ridelink.ride.entity;

import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity representing a Ride booking in RideLink.
 * Exclusively owned by Ride Management Service.
 */
@Entity
@Table(
    name = "rides",
    indexes = {
        @Index(name = "idx_ride_passenger", columnList = "passengerAccountId"),
        @Index(name = "idx_ride_driver", columnList = "driverAccountId"),
        @Index(name = "idx_ride_status", columnList = "status"),
        @Index(name = "idx_ride_created", columnList = "createdAt")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ride {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, updatable = false, length = 36)
    private String passengerAccountId;

    @Column(length = 36)
    private String driverProfileId;

    @Column(length = 36)
    private String driverAccountId;

    @Column(length = 36)
    private String vehicleId;

    @Column(nullable = false, length = 150)
    private String pickupName;

    @Column
    private Double pickupLatitude;

    @Column
    private Double pickupLongitude;

    @Column(nullable = false, length = 150)
    private String destinationName;

    @Column
    private Double destinationLatitude;

    @Column
    private Double destinationLongitude;

    @Column(nullable = false, length = 100)
    private String serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType requestedVehicleType;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedDurationMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualDistanceKm;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualDurationMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedFareAmount;

    @Column(length = 36)
    private String finalFareId;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalFareAmount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "LKR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    @Column(length = 255)
    private String cancellationReason;

    @Column(length = 36)
    private String cancelledBy;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column
    private Instant assignedAt;

    @Column
    private Instant acceptedAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column
    private Instant cancelledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (currency == null) {
            currency = "LKR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
