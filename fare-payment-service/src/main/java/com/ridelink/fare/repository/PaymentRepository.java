package com.ridelink.fare.repository;

import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByFareIdAndPaymentStatus(String fareId, PaymentStatus paymentStatus);
    boolean existsByFareIdAndPaymentStatus(String fareId, PaymentStatus paymentStatus);
    List<Payment> findByPassengerIdOrderByCreatedAtDesc(String passengerId);
    List<Payment> findByRideId(String rideId);
}
