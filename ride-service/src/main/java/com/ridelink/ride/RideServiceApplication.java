package com.ridelink.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the RideLink Ride Management Service (Port 8083).
 * Manages ride requests, lifecycle state transitions, driver discovery & reservation,
 * and interservice communication with Driver Service and Fare Service.
 */
@SpringBootApplication
public class RideServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideServiceApplication.class, args);
    }
}
