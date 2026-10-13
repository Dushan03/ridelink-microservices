package com.ridelink.driver.controller;

import com.ridelink.driver.dto.EligibleDriverResponse;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.service.DriverEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver Eligibility & Discovery", description = "Query available drivers for ride matching with Haversine geospatial proximity sorting")
public class DriverEligibilityController {

    private final DriverEligibilityService driverEligibilityService;

    public DriverEligibilityController(DriverEligibilityService driverEligibilityService) {
        this.driverEligibilityService = driverEligibilityService;
    }

    @GetMapping("/eligible")
    @Operation(
            summary = "Find Eligible Available Drivers",
            description = "Retrieves active available drivers matching service area, vehicle type, and non-expired license criteria. " +
                          "When pickup latitude and longitude are supplied, calculates Haversine distance and sorts by nearest driver."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eligible drivers retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters")
    })
    public ResponseEntity<List<EligibleDriverResponse>> getEligibleDrivers(
            @Parameter(description = "Service area name (e.g. Colombo, Kandy)")
            @RequestParam(required = false) String serviceArea,

            @Parameter(description = "Vehicle category required (CAR, VAN, THREE_WHEELER, MOTORBIKE)")
            @RequestParam(required = false) VehicleType vehicleType,

            @Parameter(description = "Pickup location latitude for distance calculation (-90 to 90)")
            @RequestParam(required = false) Double pickupLatitude,

            @Parameter(description = "Pickup location longitude for distance calculation (-180 to 180)")
            @RequestParam(required = false) Double pickupLongitude,

            @Parameter(description = "Maximum results to return (default 10, max 50)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        List<EligibleDriverResponse> drivers = driverEligibilityService.findEligibleDrivers(
                serviceArea, vehicleType, pickupLatitude, pickupLongitude, limit);
        return ResponseEntity.ok(drivers);
    }
}
