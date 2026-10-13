package com.ridelink.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.dto.ReleaseDriverRequest;
import com.ridelink.driver.dto.ReserveDriverRequest;
import com.ridelink.driver.dto.TripActionRequest;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.security.InternalServiceAuthFilter;
import com.ridelink.driver.security.JwtAuthFilter;
import com.ridelink.driver.service.DriverReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalDriverController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverReservationService driverReservationService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private InternalServiceAuthFilter internalServiceAuthFilter;

    @Test
    @DisplayName("POST /api/internal/drivers/{driverId}/reserve - 200 OK")
    void reserveDriver_Returns200() throws Exception {
        ReserveDriverRequest request = new ReserveDriverRequest("ride-999");
        DriverProfileResponse response = new DriverProfileResponse(
                "driver-1",
                "acc-1",
                "LIC-123",
                LocalDate.now().plusYears(1),
                "Colombo",
                AvailabilityStatus.RESERVED,
                6.9271,
                79.8612,
                "ride-999",
                Instant.now(),
                Instant.now()
        );

        when(driverReservationService.reserveDriver("driver-1", "ride-999")).thenReturn(response);

        mockMvc.perform(post("/api/internal/drivers/driver-1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("driver-1"))
                .andExpect(jsonPath("$.availabilityStatus").value("RESERVED"))
                .andExpect(jsonPath("$.currentRideId").value("ride-999"));
    }

    @Test
    @DisplayName("POST /api/internal/drivers/{driverId}/start-trip - 200 OK")
    void startTrip_Returns200() throws Exception {
        TripActionRequest request = new TripActionRequest("ride-999");
        DriverProfileResponse response = new DriverProfileResponse(
                "driver-1",
                "acc-1",
                "LIC-123",
                LocalDate.now().plusYears(1),
                "Colombo",
                AvailabilityStatus.ON_TRIP,
                6.9271,
                79.8612,
                "ride-999",
                Instant.now(),
                Instant.now()
        );

        when(driverReservationService.startTrip("driver-1", "ride-999")).thenReturn(response);

        mockMvc.perform(post("/api/internal/drivers/driver-1/start-trip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("ON_TRIP"));
    }

    @Test
    @DisplayName("POST /api/internal/drivers/{driverId}/release - 200 OK")
    void releaseDriver_Returns200() throws Exception {
        ReleaseDriverRequest request = new ReleaseDriverRequest("ride-999", "Trip completed");
        DriverProfileResponse response = new DriverProfileResponse(
                "driver-1",
                "acc-1",
                "LIC-123",
                LocalDate.now().plusYears(1),
                "Colombo",
                AvailabilityStatus.AVAILABLE,
                6.9271,
                79.8612,
                null,
                Instant.now(),
                Instant.now()
        );

        when(driverReservationService.releaseDriver("driver-1", "ride-999", "Trip completed")).thenReturn(response);

        mockMvc.perform(post("/api/internal/drivers/driver-1/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.currentRideId").doesNotExist());
    }
}
