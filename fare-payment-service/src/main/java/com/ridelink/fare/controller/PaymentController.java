package com.ridelink.fare.controller;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing payment processing with idempotency and transaction history endpoints.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Simulated payment processing, idempotency support, and history")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(
            summary = "Process a simulated payment for a final fare",
            description = "Simulates transaction processing. Requires Idempotency-Key header. Automatically issues receipt on success.",
            parameters = {
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Unique client-generated idempotency key (e.g. UUID) to safely retry payments without duplicates",
                            example = "e58ed763-928c-4155-bee9-fdbaaadc15f3"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment processed or idempotent replay returned"),
                    @ApiResponse(responseCode = "400", description = "Invalid payment data or missing/blank Idempotency-Key header"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Attempted to pay for another passenger's fare"),
                    @ApiResponse(responseCode = "404", description = "Fare not found"),
                    @ApiResponse(responseCode = "409", description = "Fare has already been successfully paid")
            }
    )
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, accountId, role);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Get payment transaction by ID",
            description = "Retrieves payment and digital receipt details with ownership protection.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment details found"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Payment not found")
            }
    )
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable String paymentId,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId, accountId, role));
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get all payments for the authenticated passenger",
            description = "Returns payment history ordered by creation date descending.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of payments"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
            }
    )
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        String accountId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(paymentService.getMyPayments(accountId));
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
