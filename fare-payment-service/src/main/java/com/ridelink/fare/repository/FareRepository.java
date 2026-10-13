package com.ridelink.fare.repository;

import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.enums.FareType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRepository extends MongoRepository<Fare, String> {
    Optional<Fare> findByRideIdAndFareType(String rideId, FareType fareType);
    boolean existsByRideIdAndFareType(String rideId, FareType fareType);
    List<Fare> findByPassengerId(String passengerId);
}
