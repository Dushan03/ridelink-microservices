package com.ridelink.driver.controller;

import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.dto.ReleaseDriverRequest;
import com.ridelink.driver.dto.ReserveDriverRequest;
import com.ridelink.driver.dto.TripActionRequest;
import com.ridelink.driver.service.DriverReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/drivers")
@Tag(name = "Internal Ride Integration", description = "Protected service-to-service endpoints used by Ride Management Service. Secured with X-Internal-Service-Key.")
@SecurityRequirement(name = "internalKeyAuth")
public class InternalDriverController {

    private final DriverReservationService driverReservationService;

    public InternalDriverController(DriverReservationService driverReservationService) {
        this.driverReservationService = driverReservationService;
    }

    @PostMapping("/{driverId}/reserve")
    @Operation(
            summary = "Reserve Driver (Interservice)",
            description = "Atomically transitions an AVAILABLE driver to RESERVED state and associates current rideId. " +
                          "Returns 409 Conflict if driver is no longer available. Idempotent for identical rideId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid ride ID"),
            @ApiResponse(responseCode = "401", description = "Missing internal service key"),
            @ApiResponse(responseCode = "403", description = "Invalid internal service key"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "409", description = "Driver is not in AVAILABLE state")
    })
    public ResponseEntity<DriverProfileResponse> reserveDriver(
            @PathVariable String driverId,
            @Valid @RequestBody ReserveDriverRequest request) {
        return ResponseEntity.ok(driverReservationService.reserveDriver(driverId, request.rideId()));
    }

    @PostMapping("/{driverId}/start-trip")
    @Operation(
            summary = "Start Trip (Interservice)",
            description = "Confirms ride start and transitions driver from RESERVED to ON_TRIP. Validates matching rideId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trip started successfully"),
            @ApiResponse(responseCode = "401", description = "Missing internal service key"),
            @ApiResponse(responseCode = "403", description = "Invalid internal service key"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "409", description = "Driver is not in RESERVED state or rideId mismatch")
    })
    public ResponseEntity<DriverProfileResponse> startTrip(
            @PathVariable String driverId,
            @Valid @RequestBody TripActionRequest request) {
        return ResponseEntity.ok(driverReservationService.startTrip(driverId, request.rideId()));
    }

    @PostMapping("/{driverId}/release")
    @Operation(
            summary = "Release Driver (Interservice)",
            description = "Releases driver reservation or completes trip, transitioning driver back to AVAILABLE state. " +
                          "Clears currentRideId. Supports both trip completion and cancellation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver released back to AVAILABLE"),
            @ApiResponse(responseCode = "401", description = "Missing internal service key"),
            @ApiResponse(responseCode = "403", description = "Invalid internal service key"),
            @ApiResponse(responseCode = "404", description = "Driver not found"),
            @ApiResponse(responseCode = "409", description = "Driver rideId mismatch or not in active ride state")
    })
    public ResponseEntity<DriverProfileResponse> releaseDriver(
            @PathVariable String driverId,
            @Valid @RequestBody ReleaseDriverRequest request) {
        return ResponseEntity.ok(driverReservationService.releaseDriver(driverId, request.rideId(), request.reason()));
    }
}
