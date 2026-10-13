package com.ridelink.fare.service;

import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareFinalRequest;
import com.ridelink.fare.dto.FareResponse;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.enums.FareType;
import com.ridelink.fare.exception.DuplicateFinalFareException;
import com.ridelink.fare.exception.ForbiddenOperationException;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.repository.FareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service managing fare calculations, estimates, final fares, and access control.
 */
@Service
@Transactional
public class FareService {

    private final FareRepository fareRepository;
    private final FareCalculationService calculationService;

    public FareService(FareRepository fareRepository, FareCalculationService calculationService) {
        this.fareRepository = fareRepository;
        this.calculationService = calculationService;
    }

    /**
     * Calculates an indicative fare estimate based on distance and duration.
     */
    public FareResponse calculateEstimate(FareEstimateRequest request, String optionalPassengerId) {
        BigDecimal total = calculationService.calculateTotalFare(
                request.distanceKm(),
                request.durationMinutes()
        );

        Fare estimateFare = Fare.builder()
                .id(UUID.randomUUID().toString())
                .rideId(null)
                .passengerId(optionalPassengerId)
                .distanceKm(request.distanceKm())
                .durationMinutes(request.durationMinutes())
                .baseFare(calculationService.getBaseFare())
                .ratePerKm(calculationService.getRatePerKm())
                .ratePerMinute(calculationService.getRatePerMinute())
                .totalAmount(total)
                .currency(calculationService.getCurrency())
                .fareType(FareType.ESTIMATE)
                .createdAt(Instant.now())
                .finalizedAt(null)
                .build();

        Fare saved = fareRepository.save(estimateFare);
        return FareResponse.from(saved);
    }

    /**
     * Computes and saves the FINAL fare for a completed ride.
     * Rejects duplicate final fare creation for the same ride ID (returns 409 Conflict).
     */
    public FareResponse createFinalFare(FareFinalRequest request) {
        if (fareRepository.existsByRideIdAndFareType(request.rideId(), FareType.FINAL)) {
            throw new DuplicateFinalFareException(request.rideId());
        }

        BigDecimal total = calculationService.calculateTotalFare(
                request.distanceKm(),
                request.durationMinutes()
        );

        Instant now = Instant.now();
        Fare finalFare = Fare.builder()
                .id(UUID.randomUUID().toString())
                .rideId(request.rideId())
                .passengerId(request.passengerId())
                .distanceKm(request.distanceKm())
                .durationMinutes(request.durationMinutes())
                .baseFare(calculationService.getBaseFare())
                .ratePerKm(calculationService.getRatePerKm())
                .ratePerMinute(calculationService.getRatePerMinute())
                .totalAmount(total)
                .currency(calculationService.getCurrency())
                .fareType(FareType.FINAL)
                .createdAt(now)
                .finalizedAt(now)
                .build();

        Fare saved = fareRepository.save(finalFare);
        return FareResponse.from(saved);
    }

    /**
     * Retrieves a fare record by ID with ownership check.
     */
    @Transactional(readOnly = true)
    public FareResponse getFareById(String fareId, String authenticatedAccountId, String authenticatedRole) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare", fareId));

        validateOwnership(fare, authenticatedAccountId, authenticatedRole);
        return FareResponse.from(fare);
    }

    /**
     * Retrieves the FINAL fare record for a specific ride ID with ownership check.
     */
    @Transactional(readOnly = true)
    public FareResponse getFareByRideId(String rideId, String authenticatedAccountId, String authenticatedRole) {
        Fare fare = fareRepository.findByRideIdAndFareType(rideId, FareType.FINAL)
                .orElseThrow(() -> new ResourceNotFoundException("Final fare for ride", rideId));

        validateOwnership(fare, authenticatedAccountId, authenticatedRole);
        return FareResponse.from(fare);
    }

    /**
     * Enforces that only the owning passenger, associated driver, or an administrator can access the fare.
     */
    private void validateOwnership(Fare fare, String authenticatedAccountId, String authenticatedRole) {
        if ("ADMIN".equalsIgnoreCase(authenticatedRole)) {
            return;
        }
        if (fare.getPassengerId() != null && !fare.getPassengerId().equals(authenticatedAccountId)) {
            throw new ForbiddenOperationException("Access denied: You do not have permission to view this fare");
        }
    }
}
