package com.ridelink.fare.dto;

import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.Receipt;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Response containing payment transaction outcome and receipt if successful")
public record PaymentResponse(
        @Schema(description = "Unique UUID of the payment transaction")
        String paymentId,

        @Schema(description = "Associated fare ID")
        String fareId,

        @Schema(description = "Associated ride ID")
        String rideId,

        @Schema(description = "Associated passenger ID")
        String passengerId,

        @Schema(description = "Transaction amount")
        BigDecimal amount,

        @Schema(description = "Currency code")
        String currency,

        @Schema(description = "Payment method used")
        String paymentMethod,

        @Schema(description = "Payment status (PENDING, SUCCESSFUL, FAILED)")
        String paymentStatus,

        @Schema(description = "Idempotency key supplied with the request")
        String idempotencyKey,

        @Schema(description = "Reason for failure if status is FAILED")
        String failureReason,

        @Schema(description = "Digital receipt details (present only on SUCCESSFUL payments)")
        ReceiptResponse receipt,

        @Schema(description = "Timestamp when the payment record was initiated")
        Instant createdAt,

        @Schema(description = "Timestamp when the payment was finalized")
        Instant processedAt
) {
    public static PaymentResponse from(Payment payment, Receipt receipt) {
        return new PaymentResponse(
                payment.getId(),
                payment.getFareId(),
                payment.getRideId(),
                payment.getPassengerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod().name(),
                payment.getPaymentStatus().name(),
                payment.getIdempotencyKey(),
                payment.getFailureReason(),
                ReceiptResponse.from(receipt),
                payment.getCreatedAt(),
                payment.getProcessedAt()
        );
    }
}
