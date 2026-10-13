package com.ridelink.fare.dto;

import com.ridelink.fare.entity.Receipt;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Digital receipt generated upon successful payment completion")
public record ReceiptResponse(
        @Schema(description = "Unique UUID of the receipt")
        String receiptId,

        @Schema(description = "Formatted unique receipt number", example = "RCP-8FA321BC-1727000000000")
        String receiptNumber,

        @Schema(description = "Associated payment ID")
        String paymentId,

        @Schema(description = "Associated fare ID")
        String fareId,

        @Schema(description = "Associated ride ID")
        String rideId,

        @Schema(description = "Associated passenger ID")
        String passengerId,

        @Schema(description = "Paid amount")
        BigDecimal amount,

        @Schema(description = "Currency code")
        String currency,

        @Schema(description = "Payment method used")
        String paymentMethod,

        @Schema(description = "Timestamp when the receipt was issued")
        Instant issuedAt
) {
    public static ReceiptResponse from(Receipt receipt) {
        if (receipt == null) return null;
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getPaymentId(),
                receipt.getFareId(),
                receipt.getRideId(),
                receipt.getPassengerId(),
                receipt.getAmount(),
                receipt.getCurrency(),
                receipt.getPaymentMethod().name(),
                receipt.getIssuedAt()
        );
    }
}
