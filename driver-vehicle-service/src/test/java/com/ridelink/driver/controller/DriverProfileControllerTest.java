package com.ridelink.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.CreateDriverProfileRequest;
import com.ridelink.driver.dto.DriverProfileResponse;
import com.ridelink.driver.dto.UpdateAvailabilityRequest;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.security.InternalServiceAuthFilter;
import com.ridelink.driver.security.JwtAuthFilter;
import com.ridelink.driver.service.DriverProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverProfileService driverProfileService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private InternalServiceAuthFilter internalServiceAuthFilter;

    @Test
    @DisplayName("POST /api/drivers/profile - 201 Created")
    void createProfile_Returns201() throws Exception {
        CreateDriverProfileRequest request = new CreateDriverProfileRequest(
                "LIC-12345",
                LocalDate.now().plusYears(1),
                "Colombo",
                6.9271,
                79.8612
        );

        DriverProfileResponse response = new DriverProfileResponse(
                "driver-1",
                "acc-123",
                "LIC-12345",
                LocalDate.now().plusYears(1),
                "Colombo",
                AvailabilityStatus.OFFLINE,
                6.9271,
                79.8612,
                null,
                Instant.now(),
                Instant.now()
        );

        when(driverProfileService.createProfile(any(), any(CreateDriverProfileRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/drivers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("driver-1"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC-12345"))
                .andExpect(jsonPath("$.availabilityStatus").value("OFFLINE"));
    }

    @Test
    @DisplayName("PATCH /api/drivers/me/availability - 200 OK")
    void updateAvailability_Returns200() throws Exception {
        UpdateAvailabilityRequest request = new UpdateAvailabilityRequest(AvailabilityStatus.AVAILABLE);

        DriverProfileResponse response = new DriverProfileResponse(
                "driver-1",
                "acc-123",
                "LIC-12345",
                LocalDate.now().plusYears(1),
                "Colombo",
                AvailabilityStatus.AVAILABLE,
                6.9271,
                79.8612,
                null,
                Instant.now(),
                Instant.now()
        );

        when(driverProfileService.updateAvailability(any(), any(UpdateAvailabilityRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/drivers/me/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }
}
