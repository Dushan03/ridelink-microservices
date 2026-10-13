package com.ridelink.fare.entity;

import com.ridelink.fare.enums.FareType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a calculated fare stored in MongoDB collection "fares".
 */
@Document(collection = "fares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fare {

    @Id
    private String id;

    @Indexed
    private String rideId;

    @Indexed
    private String passengerId;

    private BigDecimal distanceKm;
    private BigDecimal durationMinutes;
    private BigDecimal baseFare;
    private BigDecimal ratePerKm;
    private BigDecimal ratePerMinute;
    private BigDecimal totalAmount;
    private String currency;
    private FareType fareType;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant finalizedAt;
}
