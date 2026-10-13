package com.ridelink.driver.entity;

import com.ridelink.driver.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity representing a Vehicle linked to a Driver Profile.
 */
@Entity
@Table(
    name = "vehicles",
    indexes = {
        @Index(name = "idx_vehicle_reg_number", columnList = "registrationNumber", unique = true),
        @Index(name = "idx_vehicle_driver_prof", columnList = "driverProfileId")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String driverProfileId;

    @Column(nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private Integer manufactureYear;

    @Column(nullable = false, length = 30)
    private String colour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private Integer passengerCapacity;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
