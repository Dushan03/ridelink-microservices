package com.ridelink.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.RegisterVehicleRequest;
import com.ridelink.driver.dto.VehicleResponse;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.security.InternalServiceAuthFilter;
import com.ridelink.driver.security.JwtAuthFilter;
import com.ridelink.driver.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private InternalServiceAuthFilter internalServiceAuthFilter;

    @Test
    @DisplayName("POST /api/drivers/me/vehicles - 201 Created")
    void registerVehicle_Returns201() throws Exception {
        RegisterVehicleRequest request = new RegisterVehicleRequest(
                "WP-CAR-1234",
                "Toyota",
                "Prius",
                2020,
                "White",
                VehicleType.CAR,
                4
        );

        VehicleResponse response = new VehicleResponse(
                "veh-1",
                "driver-1",
                "WP-CAR-1234",
                "Toyota",
                "Prius",
                2020,
                "White",
                VehicleType.CAR,
                4,
                true,
                Instant.now(),
                Instant.now()
        );

        when(vehicleService.registerVehicle(any(), any(RegisterVehicleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/drivers/me/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("veh-1"))
                .andExpect(jsonPath("$.registrationNumber").value("WP-CAR-1234"));
    }

    @Test
    @DisplayName("GET /api/drivers/me/vehicles - 200 OK")
    void getVehicles_Returns200() throws Exception {
        VehicleResponse response = new VehicleResponse(
                "veh-1",
                "driver-1",
                "WP-CAR-1234",
                "Toyota",
                "Prius",
                2020,
                "White",
                VehicleType.CAR,
                4,
                true,
                Instant.now(),
                Instant.now()
        );

        when(vehicleService.getVehicles(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/drivers/me/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("veh-1"))
                .andExpect(jsonPath("$[0].make").value("Toyota"));
    }
}
