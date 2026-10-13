package com.ridelink.fare.controller;

import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing digital receipt lookups.
 */
@RestController
@RequestMapping("/api/receipts")
@Tag(name = "Receipts", description = "Digital receipt retrieval by receipt ID or payment ID")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{receiptId}")
    @Operation(
            summary = "Get digital receipt by receipt ID",
            description = "Returns receipt if caller is the owning passenger or an administrator.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Receipt found"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Receipt not found")
            }
    )
    public ResponseEntity<ReceiptResponse> getReceiptById(
            @PathVariable String receiptId,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);
        return ResponseEntity.ok(receiptService.getReceiptById(receiptId, accountId, role));
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(
            summary = "Get digital receipt by payment ID",
            description = "Returns the receipt issued for a specific successful payment transaction.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Receipt found"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
                    @ApiResponse(responseCode = "403", description = "Access denied"),
                    @ApiResponse(responseCode = "404", description = "Receipt for payment not found")
            }
    )
    public ResponseEntity<ReceiptResponse> getReceiptByPaymentId(
            @PathVariable String paymentId,
            Authentication authentication) {

        String accountId = authentication != null ? authentication.getName() : null;
        String role = extractRole(authentication);
        return ResponseEntity.ok(receiptService.getReceiptByPaymentId(paymentId, accountId, role));
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
