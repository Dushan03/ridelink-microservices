package com.ridelink.fare.controller;

import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareFinalRequest;
import com.ridelink.fare.dto.FareResponse;
import com.ridelink.fare.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing fare calculation, estimation, and finalization endpoints.
 */
@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fares", description = "Fare estimation, final calculation, and fare record retrieval")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimate")
    @Operation(
            summary = "Calculate an indicative fare estimate",
            description = "Calculates an estimate based on distance and duration without binding to a completed ride.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fare estimate calculated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error (e.g. negative distance or duration)")
            }
    )
    public ResponseEntity<FareResponse> estimateFare(
            @Valid @RequestBody FareEstimateRequest request,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(fareService.calculateEstimate(request, accountId));
    }

    @PostMapping("/final")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Compute and persist the FINAL fare for a ride",
            description = "Called upon ride completion to generate the binding final fare. Rejects duplicate final fares.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Final fare computed and saved"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or missing required fields"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "409", description = "A final fare already exists for this ride ID")
            }
    )
    public ResponseEntity<FareResponse> createFinalFare(
            @Valid @RequestBody FareFinalRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(fareService.createFinalFare(request));
    }

    @GetMapping("/{fareId}")
    @Operation(
            summary = "Get fare details by fare ID",
            description = "Returns fare details if authenticated user is the owning passenger or an administrator.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fare found"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Access denied (not your fare)"),
                    @ApiResponse(responseCode = "404", description = "Fare not found")
            }
    )
    public ResponseEntity<FareResponse> getFareById(
            @PathVariable String fareId,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);
        return ResponseEntity.ok(fareService.getFareById(fareId, accountId, role));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(
            summary = "Get final fare by ride ID",
            description = "Returns the final fare for the given ride ID with ownership protection.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Final fare found"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Final fare for ride not found")
            }
    )
    public ResponseEntity<FareResponse> getFareByRideId(
            @PathVariable String rideId,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);
        return ResponseEntity.ok(fareService.getFareByRideId(rideId, accountId, role));
    }

    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "PASSENGER";
        }
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("PASSENGER");
    }
}
