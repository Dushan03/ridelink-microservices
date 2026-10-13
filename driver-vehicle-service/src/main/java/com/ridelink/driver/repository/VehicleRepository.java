package com.ridelink.driver.repository;

import com.ridelink.driver.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    List<Vehicle> findByDriverProfileId(String driverProfileId);

    List<Vehicle> findByDriverProfileIdAndActiveTrue(String driverProfileId);

    Optional<Vehicle> findByDriverProfileIdAndId(String driverProfileId, String id);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, String id);

    long countByDriverProfileIdAndActiveTrue(String driverProfileId);
}
