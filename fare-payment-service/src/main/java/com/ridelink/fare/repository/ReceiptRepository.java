package com.ridelink.fare.repository;

import com.ridelink.fare.entity.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends MongoRepository<Receipt, String> {
    Optional<Receipt> findByPaymentId(String paymentId);
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    Optional<Receipt> findByFareId(String fareId);
    List<Receipt> findByPassengerId(String passengerId);
}
