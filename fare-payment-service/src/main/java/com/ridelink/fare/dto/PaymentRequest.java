package com.ridelink.fare.dto;

import com.ridelink.fare.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for simulating a payment against an existing final fare")
public record PaymentRequest(

        @NotBlank(message = "Fare ID is required")
        @Schema(description = "UUID of the final fare to pay", example = "c2ddec77-7a0b-2ef6-cc4d-4bb7ab160c33")
        String fareId,

        @NotNull(message = "Payment method is required (CASH, CARD, DIGITAL_WALLET)")
        @Schema(description = "Selected payment method", example = "CARD")
        PaymentMethod paymentMethod,

        @Schema(description = "Academic simulation flag: set true to force a simulated payment failure", example = "false")
        Boolean simulateFailure
) {}
