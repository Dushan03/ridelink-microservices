package com.ridelink.fare.service;

import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.enums.FareType;
import com.ridelink.fare.enums.PaymentStatus;
import com.ridelink.fare.exception.*;
import com.ridelink.fare.repository.FareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating simulated payment transactions, idempotency handling,
 * and automatic receipt generation.
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;
    private final ReceiptRepository receiptRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          FareRepository fareRepository,
                          ReceiptRepository receiptRepository) {
        this.paymentRepository = paymentRepository;
        this.fareRepository = fareRepository;
        this.receiptRepository = receiptRepository;
    }

    /**
     * Processes a payment transaction with idempotency protection and receipt issuance.
     *
     * @param request payment details (fareId, paymentMethod, simulateFailure)
     * @param idempotencyKey unique client-generated request key from Idempotency-Key header
     * @param authenticatedAccountId account ID extracted from validated JWT
     * @param authenticatedRole role of authenticated user
     * @return payment outcome and receipt (if successful)
     */
    public PaymentResponse processPayment(
            PaymentRequest request,
            String idempotencyKey,
            String authenticatedAccountId,
            String authenticatedRole) {

        if (idempotencyKey == null || idempotencyKey.trim().isBlank()) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key header cannot be null or blank");
        }
        String cleanKey = idempotencyKey.trim();

        // 1. Check idempotency: if key was already processed, return existing result
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKey(cleanKey);
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            validatePaymentOwnership(existing, authenticatedAccountId, authenticatedRole);
            Receipt receipt = receiptRepository.findByPaymentId(existing.getId()).orElse(null);
            return PaymentResponse.from(existing, receipt);
        }

        // 2. Fetch and validate Fare
        Fare fare = fareRepository.findById(request.fareId())
                .orElseThrow(() -> new ResourceNotFoundException("Fare", request.fareId()));

        if (fare.getFareType() != FareType.FINAL) {
            throw new InvalidPaymentException("Payment can only be processed for FINAL fares. Current fare type: " + fare.getFareType());
        }

        // 3. Validate Passenger Ownership
        if (!"ADMIN".equalsIgnoreCase(authenticatedRole)) {
            if (fare.getPassengerId() != null && !fare.getPassengerId().equals(authenticatedAccountId)) {
                throw new ForbiddenOperationException("Access denied: You cannot make a payment for another passenger's fare");
            }
        }

        // 4. Duplicate payment check (prevent paying an already settled fare)
        if (paymentRepository.existsByFareIdAndPaymentStatus(fare.getId(), PaymentStatus.SUCCESSFUL)) {
            throw new PaymentConflictException("Fare " + fare.getId() + " has already been successfully paid");
        }

        Instant now = Instant.now();
        String paymentId = UUID.randomUUID().toString();
        boolean isFailure = Boolean.TRUE.equals(request.simulateFailure());

        if (isFailure) {
            // Simulated Payment Failure
            Payment failedPayment = Payment.builder()
                    .id(paymentId)
                    .fareId(fare.getId())
                    .rideId(fare.getRideId() != null ? fare.getRideId() : "N/A")
                    .passengerId(fare.getPassengerId() != null ? fare.getPassengerId() : authenticatedAccountId)
                    .amount(fare.getTotalAmount())
                    .currency(fare.getCurrency())
                    .paymentMethod(request.paymentMethod())
                    .paymentStatus(PaymentStatus.FAILED)
                    .idempotencyKey(cleanKey)
                    .failureReason("Simulated payment rejection: card declined or insufficient funds")
                    .createdAt(now)
                    .processedAt(now)
                    .build();

            Payment saved = paymentRepository.save(failedPayment);
            return PaymentResponse.from(saved, null);
        } else {
            // Successful Payment & Receipt Generation
            Payment successfulPayment = Payment.builder()
                    .id(paymentId)
                    .fareId(fare.getId())
                    .rideId(fare.getRideId() != null ? fare.getRideId() : "N/A")
                    .passengerId(fare.getPassengerId() != null ? fare.getPassengerId() : authenticatedAccountId)
                    .amount(fare.getTotalAmount())
                    .currency(fare.getCurrency())
                    .paymentMethod(request.paymentMethod())
                    .paymentStatus(PaymentStatus.SUCCESSFUL)
                    .idempotencyKey(cleanKey)
                    .failureReason(null)
                    .createdAt(now)
                    .processedAt(now)
                    .build();

            Payment savedPayment = paymentRepository.save(successfulPayment);

            String receiptNumber = generateUniqueReceiptNumber();
            Receipt receipt = Receipt.builder()
                    .id(UUID.randomUUID().toString())
                    .receiptNumber(receiptNumber)
                    .paymentId(savedPayment.getId())
                    .fareId(fare.getId())
                    .rideId(fare.getRideId() != null ? fare.getRideId() : "N/A")
                    .passengerId(savedPayment.getPassengerId())
                    .amount(savedPayment.getAmount())
                    .currency(savedPayment.getCurrency())
                    .paymentMethod(savedPayment.getPaymentMethod())
                    .issuedAt(now)
                    .build();

            Receipt savedReceipt = receiptRepository.save(receipt);
            return PaymentResponse.from(savedPayment, savedReceipt);
        }
    }

    /**
     * Retrieves payment by ID with ownership enforcement.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId, String authenticatedAccountId, String authenticatedRole) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        validatePaymentOwnership(payment, authenticatedAccountId, authenticatedRole);
        Receipt receipt = receiptRepository.findByPaymentId(payment.getId()).orElse(null);
        return PaymentResponse.from(payment, receipt);
    }

    /**
     * Returns all payments belonging to the authenticated passenger.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String authenticatedAccountId) {
        List<Payment> payments = paymentRepository.findByPassengerIdOrderByCreatedAtDesc(authenticatedAccountId);
        return payments.stream()
                .map(p -> {
                    Receipt r = receiptRepository.findByPaymentId(p.getId()).orElse(null);
                    return PaymentResponse.from(p, r);
                })
                .collect(Collectors.toList());
    }

    private void validatePaymentOwnership(Payment payment, String authenticatedAccountId, String authenticatedRole) {
        if ("ADMIN".equalsIgnoreCase(authenticatedRole)) {
            return;
        }
        if (payment.getPassengerId() != null && !payment.getPassengerId().equals(authenticatedAccountId)) {
            throw new ForbiddenOperationException("Access denied: You do not have permission to access this payment");
        }
    }

    private String generateUniqueReceiptNumber() {
        return "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-" + System.currentTimeMillis();
    }
}
