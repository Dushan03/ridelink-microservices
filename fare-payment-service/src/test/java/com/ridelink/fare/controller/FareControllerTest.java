package com.ridelink.fare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare.dto.FareEstimateRequest;
import com.ridelink.fare.dto.FareFinalRequest;
import com.ridelink.fare.dto.FareResponse;
import com.ridelink.fare.exception.DuplicateFinalFareException;
import com.ridelink.fare.exception.GlobalExceptionHandler;
import com.ridelink.fare.security.JwtAuthFilter;
import com.ridelink.fare.service.FareService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FareController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FareService fareService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("POST /api/fares/estimate - 200 OK with valid inputs")
    void estimateFare_ValidInput_Returns200() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest(new BigDecimal("10.00"), new BigDecimal("15.00"));
        FareResponse response = new FareResponse(
                "est-1", null, null,
                new BigDecimal("10.00"), new BigDecimal("15.00"),
                new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("2.00"),
                new BigDecimal("630.00"), "LKR", "ESTIMATE",
                Instant.now(), null
        );

        when(fareService.calculateEstimate(any(FareEstimateRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(630.00))
                .andExpect(jsonPath("$.fareType").value("ESTIMATE"));
    }

    @Test
    @DisplayName("4. POST /api/fares/estimate - 400 Bad Request on negative distance")
    void estimateFare_NegativeDistance_Returns400() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest(new BigDecimal("-5.00"), new BigDecimal("15.00"));

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.distanceKm").exists());
    }

    @Test
    @DisplayName("5. POST /api/fares/estimate - 400 Bad Request on negative duration")
    void estimateFare_NegativeDuration_Returns400() throws Exception {
        FareEstimateRequest request = new FareEstimateRequest(new BigDecimal("5.00"), new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.durationMinutes").exists());
    }

    @Test
    @DisplayName("6. POST /api/fares/final - 400 Bad Request on missing required fields")
    void createFinalFare_MissingFields_Returns400() throws Exception {
        // Missing rideId and passengerId
        String body = "{\"distanceKm\": 10.0, \"durationMinutes\": 15.0}";

        mockMvc.perform(post("/api/fares/final")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/fares/final - 201 Created on valid request")
    void createFinalFare_Success_Returns201() throws Exception {
        FareFinalRequest request = new FareFinalRequest(
                "ride-100", "passenger-200",
                new BigDecimal("8.00"), new BigDecimal("20.00")
        );

        FareResponse response = new FareResponse(
                "fare-fin-1", "ride-100", "passenger-200",
                new BigDecimal("8.00"), new BigDecimal("20.00"),
                new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("2.00"),
                new BigDecimal("540.00"), "LKR", "FINAL",
                Instant.now(), Instant.now()
        );

        when(fareService.createFinalFare(any(FareFinalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/fares/final")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fareId").value("fare-fin-1"))
                .andExpect(jsonPath("$.rideId").value("ride-100"))
                .andExpect(jsonPath("$.fareType").value("FINAL"));
    }

    @Test
    @DisplayName("8. POST /api/fares/final - 409 Conflict on duplicate final fare")
    void createFinalFare_Duplicate_Returns409() throws Exception {
        FareFinalRequest request = new FareFinalRequest(
                "ride-100", "passenger-200",
                new BigDecimal("8.00"), new BigDecimal("20.00")
        );

        when(fareService.createFinalFare(any(FareFinalRequest.class)))
                .thenThrow(new DuplicateFinalFareException("ride-100"));

        mockMvc.perform(post("/api/fares/final")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_FINAL_FARE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ride-100")));
    }

    @Test
    @WithMockUser(username = "passenger-200", roles = {"PASSENGER"})
    @DisplayName("GET /api/fares/{fareId} - 200 OK")
    void getFareById_Success_Returns200() throws Exception {
        FareResponse response = new FareResponse(
                "fare-fin-1", "ride-100", "passenger-200",
                new BigDecimal("8.00"), new BigDecimal("20.00"),
                new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("2.00"),
                new BigDecimal("540.00"), "LKR", "FINAL",
                Instant.now(), Instant.now()
        );

        when(fareService.getFareById(eq("fare-fin-1"), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/fares/fare-fin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareId").value("fare-fin-1"));
    }
}
