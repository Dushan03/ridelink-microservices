package com.ridelink.ride.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.dto.*;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private DriverServiceClient driverServiceClient;

    @MockBean
    private FareServiceClient fareServiceClient;

    private String passengerToken;
    private String driverToken;
    private String otherPassengerToken;

    @BeforeEach
    void setUp() {
        rideRepository.deleteAll();

        passengerToken = jwtUtil.generateToken("pass-acc-123", "passenger@test.com", "ROLE_PASSENGER");
        otherPassengerToken = jwtUtil.generateToken("pass-acc-999", "other@test.com", "ROLE_PASSENGER");
        driverToken = jwtUtil.generateToken("drv-acc-456", "driver@test.com", "ROLE_DRIVER");
    }

    @Test
    @DisplayName("Full End-to-End Happy Path: Create Ride -> Reserve -> Accept -> Start -> Complete")
    void testEndToEndRideLifecycle() throws Exception {
        // 1. Mock Fare Service estimate
        FareResponseDto estimateResponse = new FareResponseDto(
                "est-1", null, "pass-acc-123",
                new BigDecimal("5.5"), new BigDecimal("20"),
                new BigDecimal("100.00"), new BigDecimal("550.00"), new BigDecimal("150.00"),
                new BigDecimal("850.00"), "LKR", "ESTIMATED", Instant.now(), null
        );
        when(fareServiceClient.calculateEstimate(eq(new BigDecimal("5.5")), eq(new BigDecimal("20"))))
                .thenReturn(estimateResponse);

        // 2. Mock Driver Service eligible drivers & reservation
        DriverEligibilityDto eligibleDriver = new DriverEligibilityDto(
                "drv-prof-456", "drv-acc-456", "COLOMBO", "AVAILABLE",
                6.9280, 79.8620, 1.1,
                "veh-100", "Toyota", "Prius", "Silver", "WP-CAB-9999",
                VehicleType.CAR, 4
        );
        when(driverServiceClient.findEligibleDrivers(eq("COLOMBO"), eq(VehicleType.CAR), eq(6.9271), eq(79.8612), anyInt()))
                .thenReturn(List.of(eligibleDriver));
        doNothing().when(driverServiceClient).reserveDriver(eq("drv-prof-456"), anyString());

        // Step 1: Create Ride (Passenger)
        CreateRideRequest createRequest = new CreateRideRequest(
                "Pettah Market", 6.9271, 79.8612,
                "Bambalapitiya Junction", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.5"), new BigDecimal("20")
        );

        String createResultJson = mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driverAccountId").value("drv-acc-456"))
                .andExpect(jsonPath("$.estimatedFareAmount").value(850.00))
                .andReturn().getResponse().getContentAsString();

        RideResponse createResponse = objectMapper.readValue(createResultJson, RideResponse.class);
        String rideId = createResponse.id();
        assertNotNull(rideId);

        // Step 2: Driver Accepts Ride (ASSIGNED -> ACCEPTED)
        mockMvc.perform(post("/api/rides/" + rideId + "/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty());

        // Step 3: Driver Starts Trip (ACCEPTED -> IN_PROGRESS)
        mockMvc.perform(post("/api/rides/" + rideId + "/start")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());

        verify(driverServiceClient).startTrip("drv-prof-456", rideId);

        // Step 4: Driver Completes Trip (IN_PROGRESS -> COMPLETED)
        FareResponseDto finalFareResponse = new FareResponseDto(
                "fare-final-999", rideId, "pass-acc-123",
                new BigDecimal("5.8"), new BigDecimal("22"),
                new BigDecimal("100.00"), new BigDecimal("600.00"), new BigDecimal("170.00"),
                new BigDecimal("920.00"), "LKR", "FINAL", Instant.now(), Instant.now()
        );
        when(fareServiceClient.createFinalFare(eq(rideId), eq("pass-acc-123"), eq(new BigDecimal("5.8")), eq(new BigDecimal("22")), any()))
                .thenReturn(finalFareResponse);

        CompleteRideRequest completeRequest = new CompleteRideRequest(new BigDecimal("5.8"), new BigDecimal("22"));

        mockMvc.perform(post("/api/rides/" + rideId + "/complete")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finalFareId").value("fare-final-999"))
                .andExpect(jsonPath("$.finalFareAmount").value(920.00))
                .andExpect(jsonPath("$.actualDistanceKm").value(5.8))
                .andExpect(jsonPath("$.actualDurationMinutes").value(22));

        verify(driverServiceClient).releaseDriver("drv-prof-456", rideId, "Trip completed");

        // Step 5: Passenger checks ride history
        mockMvc.perform(get("/api/rides/my")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rideId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        // Step 6: Other passenger cannot view this ride
        mockMvc.perform(get("/api/rides/" + rideId)
                        .header("Authorization", "Bearer " + otherPassengerToken))
                .andExpect(status().isForbidden());
    }
}
