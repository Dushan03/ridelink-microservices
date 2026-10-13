package com.ridelink.driver.controller;

import com.ridelink.driver.dto.*;
import com.ridelink.driver.service.DriverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver Profiles", description = "Driver operational profile and availability management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DriverProfileController {

    private final DriverProfileService driverProfileService;

    public DriverProfileController(DriverProfileService driverProfileService) {
        this.driverProfileService = driverProfileService;
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Create Driver Profile", description = "Creates the operational driver profile for the authenticated account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid expiry date"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden: account does not have DRIVER role"),
            @ApiResponse(responseCode = "409", description = "Driver profile or license number already exists")
    })
    public ResponseEntity<DriverProfileResponse> createProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @Valid @RequestBody CreateDriverProfileRequest request) {
        DriverProfileResponse response = driverProfileService.createProfile(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get Own Profile", description = "Retrieves the driver profile belonging to the authenticated account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found for this account")
    })
    public ResponseEntity<DriverProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId) {
        return ResponseEntity.ok(driverProfileService.getProfileByAccountId(accountId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update Own Profile", description = "Updates editable fields (service area, license expiry date) for authenticated driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<DriverProfileResponse> updateMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateDriverProfileRequest request) {
        return ResponseEntity.ok(driverProfileService.updateProfile(accountId, request));
    }

    @PatchMapping("/me/availability")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update Driver Availability", description = "Transitions availability status between OFFLINE and AVAILABLE with prerequisite verification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Invalid transition or missing prerequisites (active vehicle, location, valid license)")
    })
    public ResponseEntity<DriverProfileResponse> updateAvailability(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(driverProfileService.updateAvailability(accountId, request));
    }

    @PatchMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update Simulated Location & Service Area", description = "Updates driver service area and simulated current latitude/longitude coordinates.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (invalid coordinates)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<DriverProfileResponse> updateLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(driverProfileService.updateLocation(accountId, request));
    }

    @GetMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Driver Profile by ID (Admin)", description = "Retrieves any driver profile by its UUID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden: requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    public ResponseEntity<DriverProfileResponse> getDriverById(@PathVariable String driverId) {
        return ResponseEntity.ok(driverProfileService.getProfileById(driverId));
    }
}
