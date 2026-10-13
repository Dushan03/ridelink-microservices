package com.ridelink.ride.controller;

import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Administrator ride oversight.
 */
@RestController
@RequestMapping("/api/admin/rides")
@Tag(name = "Admin Ride Oversight", description = "Endpoints for platform administrators to view and audit all rides")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRideController {

    private final RideService rideService;

    public AdminRideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping
    @Operation(summary = "Get all rides (Admin)", description = "Retrieves all platform rides, optionally filtered by status.")
    @ApiResponse(responseCode = "200", description = "Ride list retrieved successfully")
    public ResponseEntity<List<RideResponse>> getAllRides(
            @RequestParam(required = false) RideStatus status) {
        List<RideResponse> rides = rideService.getAllRides(status);
        return ResponseEntity.ok(rides);
    }
}
