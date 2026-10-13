package com.ridelink.ride.repository;

import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, String> {

    List<Ride> findByPassengerAccountIdOrderByCreatedAtDesc(String passengerAccountId);

    List<Ride> findByDriverAccountIdOrderByCreatedAtDesc(String driverAccountId);

    List<Ride> findByStatusOrderByCreatedAtDesc(RideStatus status);

    List<Ride> findAllByOrderByCreatedAtDesc();
}
