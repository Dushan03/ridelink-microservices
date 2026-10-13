package com.ridelink.fare.dto;

import com.ridelink.fare.entity.Fare;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Response containing calculated fare details")
public record FareResponse(
        @Schema(description = "Unique UUID of the fare record")
        String fareId,

        @Schema(description = "Associated ride ID (if applicable)")
        String rideId,

        @Schema(description = "Associated passenger ID (if applicable)")
        String passengerId,

        @Schema(description = "Distance in kilometres")
        BigDecimal distanceKm,

        @Schema(description = "Duration in minutes")
        BigDecimal durationMinutes,

        @Schema(description = "Base fare rate applied")
        BigDecimal baseFare,

        @Schema(description = "Per-kilometre rate applied")
        BigDecimal ratePerKm,

        @Schema(description = "Per-minute rate applied")
        BigDecimal ratePerMinute,

        @Schema(description = "Total fare amount")
        BigDecimal totalAmount,

        @Schema(description = "Currency code (e.g. LKR)")
        String currency,

        @Schema(description = "Fare type: ESTIMATE or FINAL")
        String fareType,

        @Schema(description = "Timestamp when the fare record was created")
        Instant createdAt,

        @Schema(description = "Timestamp when the final fare was calculated")
        Instant finalizedAt
) {
    public static FareResponse from(Fare fare) {
        return new FareResponse(
                fare.getId(),
                fare.getRideId(),
                fare.getPassengerId(),
                fare.getDistanceKm(),
                fare.getDurationMinutes(),
                fare.getBaseFare(),
                fare.getRatePerKm(),
                fare.getRatePerMinute(),
                fare.getTotalAmount(),
                fare.getCurrency(),
                fare.getFareType().name(),
                fare.getCreatedAt(),
                fare.getFinalizedAt()
        );
    }
}
