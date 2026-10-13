package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Passenger and Driver ride operations.
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Ride Management", description = "Endpoints for ride creation, lifecycle transitions, driver assignment, and tracking")
@SecurityRequirement(name = "bearerAuth")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // =========================================================================
    // PASSENGER ENDPOINTS
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
    @Operation(summary = "Create ride request", description = "Creates a ride request, obtains a fare estimate from Fare Service, and attempts driver discovery/reservation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ride created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request coordinates, distance, or vehicle type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "502", description = "Fare or Driver Service unavailable")
    })
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            Authentication authentication) {
        String passengerAccountId = (String) authentication.getPrincipal();
        RideResponse response = rideService.createRide(passengerAccountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{rideId}/assign")
    @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
    @Operation(summary = "Retry driver assignment", description = "Retries finding and reserving an available driver for a ride currently in REQUESTED status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver successfully assigned and reserved"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Ride not found"),
            @ApiResponse(responseCode = "409", description = "No available driver found or invalid status transition")
    })
    public ResponseEntity<RideResponse> assignDriver(
            @PathVariable String rideId,
            Authentication authentication) {
        String passengerAccountId = (String) authentication.getPrincipal();
        RideResponse response = rideService.assignDriver(rideId, passengerAccountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
    @Operation(summary = "Get passenger ride history", description = "Retrieves all ride requests created by the authenticated passenger.")
    @ApiResponse(responseCode = "200", description = "Ride list retrieved successfully")
    public ResponseEntity<List<RideResponse>> getMyPassengerRides(Authentication authentication) {
        String passengerAccountId = (String) authentication.getPrincipal();
        List<RideResponse> rides = rideService.getPassengerRides(passengerAccountId);
        return ResponseEntity.ok(rides);
    }

    // =========================================================================
    // SHARED / GENERAL ENDPOINTS
    // =========================================================================

    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride by ID", description = "Retrieves details of a specific ride. Accessible only by the passenger, assigned driver, or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied: user is not the passenger, assigned driver, or admin"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    public ResponseEntity<RideResponse> getRideById(
            @PathVariable String rideId,
            Authentication authentication) {
        String callerAccountId = (String) authentication.getPrincipal();
        String callerRole = getPrimaryRole(authentication);
        RideResponse response = rideService.getRideById(rideId, callerAccountId, callerRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rideId}/cancel")
    @Operation(summary = "Cancel ride", description = "Cancels a ride in REQUESTED, ASSIGNED, or ACCEPTED state. Automatically releases any reserved driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Ride not found"),
            @ApiResponse(responseCode = "409", description = "Cannot cancel ride in IN_PROGRESS or COMPLETED state")
    })
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable String rideId,
            @RequestBody(required = false) CancelRideRequest request,
            Authentication authentication) {
        String callerAccountId = (String) authentication.getPrincipal();
        String callerRole = getPrimaryRole(authentication);
        String reason = (request != null) ? request.reason() : "Cancelled by user";
        RideResponse response = rideService.cancelRide(rideId, callerAccountId, callerRole, reason);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DRIVER ENDPOINTS
    // =========================================================================

    @GetMapping("/driver/my")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get driver assigned rides", description = "Retrieves all rides assigned to the authenticated driver.")
    @ApiResponse(responseCode = "200", description = "Assigned rides retrieved successfully")
    public ResponseEntity<List<RideResponse>> getMyDriverRides(Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        List<RideResponse> rides = rideService.getDriverRides(driverAccountId);
        return ResponseEntity.ok(rides);
    }

    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accept assigned ride", description = "Driver accepts an assigned ride (ASSIGNED -> ACCEPTED).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride accepted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied: caller is not the assigned driver"),
            @ApiResponse(responseCode = "404", description = "Ride not found"),
            @ApiResponse(responseCode = "409", description = "Ride is not in ASSIGNED state")
    })
    public ResponseEntity<RideResponse> acceptRide(
            @PathVariable String rideId,
            Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        RideResponse response = rideService.acceptRide(rideId, driverAccountId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Start accepted ride", description = "Driver starts the trip (ACCEPTED -> IN_PROGRESS), updating Driver Service status to ON_TRIP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip started successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied: caller is not the assigned driver"),
            @ApiResponse(responseCode = "404", description = "Ride not found"),
            @ApiResponse(responseCode = "409", description = "Ride is not in ACCEPTED state")
    })
    public ResponseEntity<RideResponse> startRide(
            @PathVariable String rideId,
            Authentication authentication) {
        String driverAccountId = (String) authentication.getPrincipal();
        RideResponse response = rideService.startRide(rideId, driverAccountId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Complete trip", description = "Driver completes the trip (IN_PROGRESS -> COMPLETED), calculates final fare with Fare Service, and releases driver back to AVAILABLE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip completed and final fare generated"),
            @ApiResponse(responseCode = "400", description = "Invalid actual distance or duration"),
            @ApiResponse(responseCode = "403", description = "Access denied: caller is not the assigned driver"),
            @ApiResponse(responseCode = "404", description = "Ride not found"),
            @ApiResponse(responseCode = "409", description = "Ride is not in IN_PROGRESS state")
    })
    public ResponseEntity<RideResponse> completeRide(
            @PathVariable String rideId,
            @Valid @RequestBody CompleteRideRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String driverAccountId = (String) authentication.getPrincipal();
        String authHeader = httpRequest.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        RideResponse response = rideService.completeRide(rideId, driverAccountId, request, token);
        return ResponseEntity.ok(response);
    }

    // Helper
    private String getPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PASSENGER");
    }
}
