package com.ridelink.ride.controller;

import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.security.JwtUtil;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RideService rideService;

    private String adminToken;
    private String passengerToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin-acc-1", "admin@ridelink.com", "ROLE_ADMIN");
        passengerToken = jwtUtil.generateToken("pass-acc-1", "pass@ridelink.com", "ROLE_PASSENGER");
    }

    @Test
    @DisplayName("GET /api/admin/rides - Admin can view all platform rides")
    void testAdminCanGetAllRides() throws Exception {
        Ride ride = Ride.builder()
                .id("ride-admin-1")
                .passengerAccountId("pass-1")
                .driverProfileId("drv-1")
                .driverAccountId("drv-acc-1")
                .vehicleId("veh-1")
                .pickupName("Colombo Fort")
                .pickupLatitude(6.9344)
                .pickupLongitude(79.8500)
                .destinationName("Bambalapitiya")
                .destinationLatitude(6.8900)
                .destinationLongitude(79.8550)
                .serviceArea("COLOMBO")
                .requestedVehicleType(VehicleType.CAR)
                .estimatedDistanceKm(new BigDecimal("5.0"))
                .estimatedDurationMinutes(new BigDecimal("15"))
                .estimatedFareAmount(new BigDecimal("750.00"))
                .currency("LKR")
                .status(RideStatus.COMPLETED)
                .requestedAt(Instant.now())
                .build();

        when(rideService.getAllRides(any())).thenReturn(List.of(RideResponse.from(ride)));

        mockMvc.perform(get("/api/admin/rides")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ride-admin-1"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/admin/rides - Non-admin user gets 403 Forbidden")
    void testNonAdminCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/rides")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isForbidden());
    }
}
