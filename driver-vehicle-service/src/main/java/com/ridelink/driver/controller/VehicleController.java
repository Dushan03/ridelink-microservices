package com.ridelink.driver.controller;

import com.ridelink.driver.dto.RegisterVehicleRequest;
import com.ridelink.driver.dto.UpdateVehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.service.VehicleService;
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

import java.util.List;

@RestController
@RequestMapping("/api/drivers/me/vehicles")
@Tag(name = "Vehicles", description = "Driver vehicle registration and fleet management APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DRIVER')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @Operation(summary = "Register Vehicle", description = "Registers a new vehicle belonging to the authenticated driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Driver profile not found"),
            @ApiResponse(responseCode = "409", description = "Vehicle registration number already exists")
    })
    public ResponseEntity<VehicleResponse> registerVehicle(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @Valid @RequestBody RegisterVehicleRequest request) {
        VehicleResponse response = vehicleService.registerVehicle(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get Own Vehicles", description = "Lists all vehicles registered under the authenticated driver's profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicles retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Driver profile not found")
    })
    public ResponseEntity<List<VehicleResponse>> getVehicles(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId) {
        return ResponseEntity.ok(vehicleService.getVehicles(accountId));
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get Vehicle by ID", description = "Retrieves an owned vehicle by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden: vehicle belongs to another driver"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponse> getVehicleById(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleById(accountId, vehicleId));
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "Update Vehicle", description = "Updates an owned vehicle's make, model, year, colour, type, or capacity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden: vehicle belongs to another driver"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponse> updateVehicle(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(accountId, vehicleId, request));
    }

    @PatchMapping("/{vehicleId}/active")
    @Operation(summary = "Set Vehicle Active State", description = "Activates or deactivates an owned vehicle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle active state updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden: vehicle belongs to another driver"),
            @ApiResponse(responseCode = "404", description = "Vehicle not found")
    })
    public ResponseEntity<VehicleResponse> setVehicleActive(
            @Parameter(hidden = true) @AuthenticationPrincipal String accountId,
            @PathVariable String vehicleId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(vehicleService.setVehicleActive(accountId, vehicleId, active));
    }
}
