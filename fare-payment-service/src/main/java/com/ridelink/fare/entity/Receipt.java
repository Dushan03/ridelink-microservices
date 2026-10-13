package com.ridelink.fare.entity;

import com.ridelink.fare.enums.PaymentMethod;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a digital receipt stored in MongoDB collection "receipts".
 */
@Document(collection = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    private String id;

    @Indexed(unique = true)
    private String receiptNumber;

    @Indexed(unique = true)
    private String paymentId;

    @Indexed
    private String fareId;

    @Indexed
    private String rideId;

    @Indexed
    private String passengerId;

    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;

    @Builder.Default
    private Instant issuedAt = Instant.now();
}
