package com.ridelink.driver.controller;

import com.ridelink.driver.dto.EligibleDriverResponse;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.enums.VehicleType;
import com.ridelink.driver.security.InternalServiceAuthFilter;
import com.ridelink.driver.security.JwtAuthFilter;
import com.ridelink.driver.service.DriverEligibilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverEligibilityController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverEligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverEligibilityService driverEligibilityService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private InternalServiceAuthFilter internalServiceAuthFilter;

    @Test
    @DisplayName("GET /api/drivers/eligible - 200 OK")
    void getEligibleDrivers_Returns200() throws Exception {
        EligibleDriverResponse driver = new EligibleDriverResponse(
                "driver-1",
                "acc-1",
                "Colombo",
                AvailabilityStatus.AVAILABLE,
                6.9271,
                79.8612,
                1.5,
                "veh-1",
                "Toyota",
                "Prius",
                "White",
                "WP-CAR-1234",
                VehicleType.CAR,
                4
        );

        when(driverEligibilityService.findEligibleDrivers(any(), any(), any(), any(), any()))
                .thenReturn(List.of(driver));

        mockMvc.perform(get("/api/drivers/eligible")
                        .param("serviceArea", "Colombo")
                        .param("vehicleType", "CAR")
                        .param("pickupLatitude", "6.9270")
                        .param("pickupLongitude", "79.8610"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].driverProfileId").value("driver-1"))
                .andExpect(jsonPath("$[0].vehicleRegistrationNumber").value("WP-CAR-1234"))
                .andExpect(jsonPath("$[0].estimatedDistanceKm").value(1.5));
    }
}
