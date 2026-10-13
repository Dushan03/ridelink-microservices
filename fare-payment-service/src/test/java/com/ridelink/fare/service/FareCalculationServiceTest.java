package com.ridelink.fare.service;

import com.ridelink.fare.config.FareRateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareCalculationServiceTest {

    private FareCalculationService calculationService;
    private FareRateProperties rateProperties;

    @BeforeEach
    void setUp() {
        rateProperties = new FareRateProperties();
        rateProperties.setBaseFare(new BigDecimal("100.00"));
        rateProperties.setRatePerKm(new BigDecimal("50.00"));
        rateProperties.setRatePerMinute(new BigDecimal("2.00"));
        rateProperties.setCurrency("LKR");

        calculationService = new FareCalculationService(rateProperties);
    }

    @Test
    @DisplayName("1. Correct fare calculation with standard formula")
    void testStandardFareCalculation() {
        // baseFare=100 + (10km * 50) + (15min * 2) = 100 + 500 + 30 = 630.00
        BigDecimal distance = new BigDecimal("10.00");
        BigDecimal duration = new BigDecimal("15.00");

        BigDecimal total = calculationService.calculateTotalFare(distance, duration);

        assertThat(total).isEqualByComparingTo("630.00");
    }

    @Test
    @DisplayName("2. Correct two-decimal HALF_UP rounding")
    void testTwoDecimalRounding() {
        // baseFare=100 + (3.333km * 50) + (7.777min * 2) = 100 + 166.65 + 15.554 = 282.204 -> 282.20
        BigDecimal distance = new BigDecimal("3.333");
        BigDecimal duration = new BigDecimal("7.777");

        BigDecimal total = calculationService.calculateTotalFare(distance, duration);

        assertThat(total).isEqualByComparingTo("282.20");
        assertThat(total.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("3. Zero distance and zero duration yields base fare")
    void testZeroDistanceAndZeroDuration() {
        BigDecimal distance = BigDecimal.ZERO;
        BigDecimal duration = BigDecimal.ZERO;

        BigDecimal total = calculationService.calculateTotalFare(distance, duration);

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("4. Negative distance rejection")
    void testNegativeDistanceRejection() {
        BigDecimal distance = new BigDecimal("-1.50");
        BigDecimal duration = new BigDecimal("10.00");

        assertThatThrownBy(() -> calculationService.calculateTotalFare(distance, duration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distance in kilometres cannot be null or negative");
    }

    @Test
    @DisplayName("5. Negative duration rejection")
    void testNegativeDurationRejection() {
        BigDecimal distance = new BigDecimal("10.00");
        BigDecimal duration = new BigDecimal("-5.00");

        assertThatThrownBy(() -> calculationService.calculateTotalFare(distance, duration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration in minutes cannot be null or negative");
    }
}
