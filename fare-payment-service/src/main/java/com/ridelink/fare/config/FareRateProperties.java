package com.ridelink.fare.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configurable fare rate properties mapped from application.properties (prefix: fare.rates).
 */
@Configuration
@ConfigurationProperties(prefix = "fare.rates")
@Getter
@Setter
public class FareRateProperties {
    private BigDecimal baseFare = new BigDecimal("100.00");
    private BigDecimal ratePerKm = new BigDecimal("50.00");
    private BigDecimal ratePerMinute = new BigDecimal("2.00");
    private String currency = "LKR";
}
