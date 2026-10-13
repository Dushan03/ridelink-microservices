package com.ridelink.fare.service;

import com.ridelink.fare.config.FareRateProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure calculation service for computing ride fares based on configurable rates.
 * Formula: fare = baseFare + (distanceKm * ratePerKm) + (durationMinutes * ratePerMinute)
 */
@Service
public class FareCalculationService {

    private final FareRateProperties rateProperties;

    public FareCalculationService(FareRateProperties rateProperties) {
        this.rateProperties = rateProperties;
    }

    /**
     * Computes the total fare using configured rates, rounding to 2 decimal places with HALF_UP.
     *
     * @param distanceKm ride distance in kilometres (must be non-negative)
     * @param durationMinutes ride duration in minutes (must be non-negative)
     * @return calculated total amount as BigDecimal with scale 2
     */
    public BigDecimal calculateTotalFare(BigDecimal distanceKm, BigDecimal durationMinutes) {
        return calculateTotalFare(
                distanceKm,
                durationMinutes,
                rateProperties.getBaseFare(),
                rateProperties.getRatePerKm(),
                rateProperties.getRatePerMinute()
        );
    }

    /**
     * Overloaded method permitting explicit rate parameters for custom pricing or testing.
     */
    public BigDecimal calculateTotalFare(
            BigDecimal distanceKm,
            BigDecimal durationMinutes,
            BigDecimal baseFare,
            BigDecimal ratePerKm,
            BigDecimal ratePerMinute) {

        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Distance in kilometres cannot be null or negative");
        }
        if (durationMinutes == null || durationMinutes.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Duration in minutes cannot be null or negative");
        }

        BigDecimal distanceCharge = distanceKm.multiply(ratePerKm);
        BigDecimal durationCharge = durationMinutes.multiply(ratePerMinute);

        BigDecimal total = baseFare
                .add(distanceCharge)
                .add(durationCharge)
                .setScale(2, RoundingMode.HALF_UP);

        return total;
    }

    public BigDecimal getBaseFare() {
        return rateProperties.getBaseFare().setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRatePerKm() {
        return rateProperties.getRatePerKm().setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getRatePerMinute() {
        return rateProperties.getRatePerMinute().setScale(2, RoundingMode.HALF_UP);
    }

    public String getCurrency() {
        return rateProperties.getCurrency();
    }
}
