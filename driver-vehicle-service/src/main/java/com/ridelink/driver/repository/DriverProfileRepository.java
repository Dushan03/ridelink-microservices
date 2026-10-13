package com.ridelink.driver.repository;

import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, String> {

    Optional<DriverProfile> findByAccountId(String accountId);

    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);

    boolean existsByAccountId(String accountId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<DriverProfile> findByAvailabilityStatus(AvailabilityStatus availabilityStatus);

    List<DriverProfile> findByAvailabilityStatusAndServiceAreaIgnoreCase(AvailabilityStatus availabilityStatus, String serviceArea);
}
