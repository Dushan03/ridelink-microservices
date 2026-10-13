package com.ridelink.fare.entity;

import com.ridelink.fare.enums.PaymentMethod;
import com.ridelink.fare.enums.PaymentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a payment transaction stored in MongoDB collection "payments".
 */
@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Indexed
    private String fareId;

    @Indexed
    private String rideId;

    @Indexed
    private String passengerId;

    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String failureReason;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant processedAt;
}
