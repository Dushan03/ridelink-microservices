package com.ridelink.fare.service;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.enums.FareType;
import com.ridelink.fare.enums.PaymentMethod;
import com.ridelink.fare.enums.PaymentStatus;
import com.ridelink.fare.exception.*;
import com.ridelink.fare.repository.FareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Fare finalFare;

    @BeforeEach
    void setUp() {
        finalFare = Fare.builder()
                .id("fare-123")
                .rideId("ride-456")
                .passengerId("passenger-789")
                .distanceKm(new BigDecimal("10.00"))
                .durationMinutes(new BigDecimal("20.00"))
                .baseFare(new BigDecimal("100.00"))
                .ratePerKm(new BigDecimal("50.00"))
                .ratePerMinute(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("640.00"))
                .currency("LKR")
                .fareType(FareType.FINAL)
                .createdAt(Instant.now())
                .finalizedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("9. Successful cash payment generates receipt")
    void testSuccessfulCashPayment() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CASH, false);
        String idempotencyKey = "key-cash-1";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-123", PaymentStatus.SUCCESSFUL)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo("SUCCESSFUL");
        assertThat(response.paymentMethod()).isEqualTo("CASH");
        assertThat(response.receipt()).isNotNull();
        assertThat(response.receipt().receiptNumber()).startsWith("RCP-");
        assertThat(response.receipt().amount()).isEqualByComparingTo("640.00");
    }

    @Test
    @DisplayName("10. Successful card payment generates receipt")
    void testSuccessfulCardPayment() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, false);
        String idempotencyKey = "key-card-1";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-123", PaymentStatus.SUCCESSFUL)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo("SUCCESSFUL");
        assertThat(response.paymentMethod()).isEqualTo("CARD");
        assertThat(response.receipt()).isNotNull();
    }

    @Test
    @DisplayName("11. Successful digital wallet payment generates receipt")
    void testSuccessfulDigitalWalletPayment() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.DIGITAL_WALLET, false);
        String idempotencyKey = "key-wallet-1";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-123", PaymentStatus.SUCCESSFUL)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo("SUCCESSFUL");
        assertThat(response.paymentMethod()).isEqualTo("DIGITAL_WALLET");
        assertThat(response.receipt()).isNotNull();
    }

    @Test
    @DisplayName("12 & 14. Simulated payment failure produces FAILED status and NO receipt")
    void testSimulatedPaymentFailure() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, true);
        String idempotencyKey = "key-fail-1";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-123", PaymentStatus.SUCCESSFUL)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.paymentStatus()).isEqualTo("FAILED");
        assertThat(response.failureReason()).isNotEmpty();
        assertThat(response.receipt()).isNull();

        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    @DisplayName("15. Idempotency-key retry returns existing payment result")
    void testIdempotencyKeyRetry_ReturnsExisting() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, false);
        String idempotencyKey = "key-repeat-1";

        Payment existing = Payment.builder()
                .id("pay-existing-99")
                .fareId("fare-123")
                .rideId("ride-456")
                .passengerId("passenger-789")
                .amount(new BigDecimal("640.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.SUCCESSFUL)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .processedAt(Instant.now())
                .build();

        Receipt existingReceipt = Receipt.builder()
                .id("rcp-existing-1")
                .receiptNumber("RCP-EXISTING-123")
                .paymentId("pay-existing-99")
                .fareId("fare-123")
                .rideId("ride-456")
                .passengerId("passenger-789")
                .amount(new BigDecimal("640.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .issuedAt(Instant.now())
                .build();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));
        when(receiptRepository.findByPaymentId("pay-existing-99")).thenReturn(Optional.of(existingReceipt));

        PaymentResponse response = paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.paymentId()).isEqualTo("pay-existing-99");
        assertThat(response.receipt().receiptNumber()).isEqualTo("RCP-EXISTING-123");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("16. Invalid or blank idempotency key rejected")
    void testBlankIdempotencyKey_ThrowsException() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, false);

        assertThatThrownBy(() -> paymentService.processPayment(request, "   ", "passenger-789", "PASSENGER"))
                .isInstanceOf(InvalidIdempotencyKeyException.class)
                .hasMessageContaining("Idempotency-Key header cannot be null or blank");
    }

    @Test
    @DisplayName("20. Paying another passenger's fare throws Forbidden (403)")
    void testPayOtherPassengerFare_ThrowsForbidden() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, false);
        String idempotencyKey = "key-unauth-1";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));

        assertThatThrownBy(() -> paymentService.processPayment(request, idempotencyKey, "intruder-id", "PASSENGER"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Access denied: You cannot make a payment for another passenger");
    }

    @Test
    @DisplayName("Duplicate payment on already paid fare throws Conflict (409)")
    void testDuplicatePaymentOnPaidFare_ThrowsConflict() {
        PaymentRequest request = new PaymentRequest("fare-123", PaymentMethod.CARD, false);
        String idempotencyKey = "new-key-for-same-fare";

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-123", PaymentStatus.SUCCESSFUL)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(request, idempotencyKey, "passenger-789", "PASSENGER"))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("has already been successfully paid");
    }
}
