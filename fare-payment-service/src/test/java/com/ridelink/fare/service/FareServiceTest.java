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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    @Mock
    private FareCalculationService calculationService;

    @InjectMocks
    private FareService fareService;

    private Fare finalFare;

    @BeforeEach
    void setUp() {
        finalFare = Fare.builder()
                .id("fare-123")
                .rideId("ride-456")
                .passengerId("passenger-789")
                .distanceKm(new BigDecimal("10.00"))
                .durationMinutes(new BigDecimal("20.00"))
                .baseFare(new BigDecimal("100.00"))
                .ratePerKm(new BigDecimal("50.00"))
                .ratePerMinute(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("640.00"))
                .currency("LKR")
                .fareType(FareType.FINAL)
                .createdAt(Instant.now())
                .finalizedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Calculate estimate returns valid FareResponse")
    void testCalculateEstimate() {
        FareEstimateRequest request = new FareEstimateRequest(new BigDecimal("5.00"), new BigDecimal("10.00"));

        when(calculationService.calculateTotalFare(request.distanceKm(), request.durationMinutes()))
                .thenReturn(new BigDecimal("370.00"));
        when(calculationService.getBaseFare()).thenReturn(new BigDecimal("100.00"));
        when(calculationService.getRatePerKm()).thenReturn(new BigDecimal("50.00"));
        when(calculationService.getRatePerMinute()).thenReturn(new BigDecimal("2.00"));
        when(calculationService.getCurrency()).thenReturn("LKR");
        when(fareRepository.save(any(Fare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FareResponse response = fareService.calculateEstimate(request, "passenger-789");

        assertThat(response).isNotNull();
        assertThat(response.totalAmount()).isEqualByComparingTo("370.00");
        assertThat(response.fareType()).isEqualTo("ESTIMATE");
        assertThat(response.currency()).isEqualTo("LKR");
    }

    @Test
    @DisplayName("7. Creation of a final fare")
    void testCreateFinalFare_Success() {
        FareFinalRequest request = new FareFinalRequest(
                "ride-456",
                "passenger-789",
                new BigDecimal("10.00"),
                new BigDecimal("20.00")
        );

        when(fareRepository.existsByRideIdAndFareType("ride-456", FareType.FINAL)).thenReturn(false);
        when(calculationService.calculateTotalFare(request.distanceKm(), request.durationMinutes()))
                .thenReturn(new BigDecimal("640.00"));
        when(calculationService.getBaseFare()).thenReturn(new BigDecimal("100.00"));
        when(calculationService.getRatePerKm()).thenReturn(new BigDecimal("50.00"));
        when(calculationService.getRatePerMinute()).thenReturn(new BigDecimal("2.00"));
        when(calculationService.getCurrency()).thenReturn("LKR");
        when(fareRepository.save(any(Fare.class))).thenReturn(finalFare);

        FareResponse response = fareService.createFinalFare(request);

        assertThat(response).isNotNull();
        assertThat(response.fareId()).isEqualTo("fare-123");
        assertThat(response.rideId()).isEqualTo("ride-456");
        assertThat(response.totalAmount()).isEqualByComparingTo("640.00");
        assertThat(response.fareType()).isEqualTo("FINAL");
    }

    @Test
    @DisplayName("8. Prevention of duplicate final fares throws DuplicateFinalFareException (409)")
    void testCreateFinalFare_Duplicate_ThrowsConflict() {
        FareFinalRequest request = new FareFinalRequest(
                "ride-456",
                "passenger-789",
                new BigDecimal("10.00"),
                new BigDecimal("20.00")
        );

        when(fareRepository.existsByRideIdAndFareType("ride-456", FareType.FINAL)).thenReturn(true);

        assertThatThrownBy(() -> fareService.createFinalFare(request))
                .isInstanceOf(DuplicateFinalFareException.class)
                .hasMessageContaining("A final fare has already been calculated");

        verify(fareRepository, never()).save(any(Fare.class));
    }

    @Test
    @DisplayName("20. Passenger ownership enforcement allows owner")
    void testGetFareById_OwnerSuccess() {
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));

        FareResponse response = fareService.getFareById("fare-123", "passenger-789", "PASSENGER");

        assertThat(response).isNotNull();
        assertThat(response.fareId()).isEqualTo("fare-123");
    }

    @Test
    @DisplayName("20. Passenger ownership enforcement forbids other passenger (403)")
    void testGetFareById_UnauthorizedPassenger_ThrowsForbidden() {
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));

        assertThatThrownBy(() -> fareService.getFareById("fare-123", "different-passenger", "PASSENGER"))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("21. Administrator access permitted for any fare")
    void testGetFareById_Admin_Success() {
        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(finalFare));

        FareResponse response = fareService.getFareById("fare-123", "admin-account-id", "ADMIN");

        assertThat(response).isNotNull();
        assertThat(response.fareId()).isEqualTo("fare-123");
    }

    @Test
    @DisplayName("22. Resource not found when fare does not exist (404)")
    void testGetFareById_NotFound_ThrowsException() {
        when(fareRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fareService.getFareById("non-existent", "passenger-789", "PASSENGER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fare not found");
    }
}
