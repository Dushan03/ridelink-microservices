package com.ridelink.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the RideLink Driver & Vehicle Service (Port 8082).
 * Handles driver operational profiles, vehicle registration, availability lifecycle,
 * eligible driver retrieval (Haversine distance), and internal ride reservation operations.
 */
@SpringBootApplication
public class DriverVehicleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverVehicleServiceApplication.class, args);
    }
}
