package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CompleteRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.entity.Ride;
import com.ridelink.ride.enums.RideStatus;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.exception.InvalidRideTransitionException;
import com.ridelink.ride.exception.NoAvailableDriverException;
import com.ridelink.ride.exception.RideAccessDeniedException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.security.JwtUtil;
import com.ridelink.ride.service.RideService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RideService rideService;

    private String passengerToken;
    private String driverToken;
    private String adminToken;
    private RideResponse sampleResponse;

    @BeforeEach
    void setUp() {
        passengerToken = jwtUtil.generateToken("pass-acc-1", "passenger@test.com", "ROLE_PASSENGER");
        driverToken = jwtUtil.generateToken("drv-acc-1", "driver@test.com", "ROLE_DRIVER");
        adminToken = jwtUtil.generateToken("admin-acc-1", "admin@test.com", "ROLE_ADMIN");

        Ride ride = Ride.builder()
                .id("ride-100")
                .passengerAccountId("pass-acc-1")
                .driverProfileId("drv-prof-1")
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
                .actualDistanceKm(new BigDecimal("5.0"))
                .actualDurationMinutes(new BigDecimal("15"))
                .estimatedFareAmount(new BigDecimal("750.00"))
                .finalFareId("fare-final-1")
                .finalFareAmount(new BigDecimal("750.00"))
                .currency("LKR")
                .status(RideStatus.REQUESTED)
                .requestedAt(Instant.now())
                .build();

        sampleResponse = RideResponse.from(ride);
    }

    // 1. Valid Ride Creation
    @Test
    @DisplayName("POST /api/rides - Valid ride request creates 201 response")
    void testCreateRideSuccess() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "Colombo Fort", 6.9344, 79.8500,
                "Bambalapitiya", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.0"), new BigDecimal("15")
        );

        when(rideService.createRide(eq("pass-acc-1"), any(CreateRideRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ride-100"))
                .andExpect(jsonPath("$.pickupName").value("Colombo Fort"))
                .andExpect(jsonPath("$.passengerAccountId").value("pass-acc-1"));
    }

    // 3. Blank pickup rejection (400 validation error)
    @Test
    @DisplayName("POST /api/rides - Blank pickup returns 400 with fieldErrors")
    void testCreateRideBlankPickup() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "", 6.9344, 79.8500,
                "Bambalapitiya", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.0"), new BigDecimal("15")
        );

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.pickupName").exists());
    }

    // 4. Blank destination rejection
    @Test
    @DisplayName("POST /api/rides - Blank destination returns 400")
    void testCreateRideBlankDestination() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "Colombo Fort", 6.9344, 79.8500,
                "   ", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.0"), new BigDecimal("15")
        );

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.destinationName").exists());
    }

    // 5. Identical pickup and destination rejection
    @Test
    @DisplayName("POST /api/rides - Identical pickup and destination throws 400")
    void testCreateRideIdenticalPickupAndDestination() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "Same Place", 6.9344, 79.8500,
                "Same Place", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.0"), new BigDecimal("15")
        );

        when(rideService.createRide(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Pickup and destination locations cannot be identical"));

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Pickup and destination locations cannot be identical"));
    }

    // 6. Negative distance rejection & 7. Negative duration rejection
    @Test
    @DisplayName("POST /api/rides - Negative distance returns 400")
    void testCreateRideNegativeDistance() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "Colombo Fort", 6.9344, 79.8500,
                "Bambalapitiya", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("-5.0"), new BigDecimal("15")
        );

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.estimatedDistanceKm").exists());
    }

    // 8. Invalid coordinates
    @Test
    @DisplayName("POST /api/rides - Latitude > 90 returns 400")
    void testCreateRideInvalidLatitude() throws Exception {
        CreateRideRequest request = new CreateRideRequest(
                "Colombo Fort", 100.0, 79.8500,
                "Bambalapitiya", 6.8900, 79.8550,
                "COLOMBO", VehicleType.CAR,
                new BigDecimal("5.0"), new BigDecimal("15")
        );

        mockMvc.perform(post("/api/rides")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pickupLatitude").exists());
    }

    // 39. Missing JWT -> 401
    @Test
    @DisplayName("GET /api/rides/my - Missing token returns 401")
    void testMissingJwt() throws Exception {
        mockMvc.perform(get("/api/rides/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // 40. Invalid JWT -> 401
    @Test
    @DisplayName("GET /api/rides/my - Invalid token returns 401")
    void testInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/rides/my")
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // 46. Conflict-error response structure (409)
    @Test
    @DisplayName("POST /api/rides/{id}/accept - Invalid transition returns 409 Conflict")
    void testInvalidTransitionConflict() throws Exception {
        when(rideService.acceptRide(eq("ride-100"), eq("drv-acc-1")))
                .thenThrow(new InvalidRideTransitionException("Cannot accept ride: status must be ASSIGNED"));

        mockMvc.perform(post("/api/rides/ride-100/accept")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INVALID_RIDE_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Cannot accept ride: status must be ASSIGNED"));
    }

    // 44. Resource-not-found response (404)
    @Test
    @DisplayName("GET /api/rides/{id} - Not found returns 404")
    void testRideNotFound() throws Exception {
        when(rideService.getRideById(eq("non-existent"), anyString(), anyString()))
                .thenThrow(new RideNotFoundException("non-existent"));

        mockMvc.perform(get("/api/rides/non-existent")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // Role-based authorization test: Passenger cannot call driver accept endpoint
    @Test
    @DisplayName("POST /api/rides/{id}/accept - Forbidden for Passenger (403)")
    void testPassengerCannotAcceptRideRoleForbidden() throws Exception {
        mockMvc.perform(post("/api/rides/ride-100/accept")
                        .header("Authorization", "Bearer " + passengerToken))
                .andExpect(status().isForbidden());
    }

    // Driver Complete Ride Endpoint
    @Test
    @DisplayName("POST /api/rides/{id}/complete - Driver completes trip")
    void testDriverCompleteRideEndpoint() throws Exception {
        CompleteRideRequest req = new CompleteRideRequest(new BigDecimal("5.5"), new BigDecimal("18"));
        when(rideService.completeRide(eq("ride-100"), eq("drv-acc-1"), any(), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/rides/ride-100/complete")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ride-100"));
    }

    // Driver Start Ride Endpoint
    @Test
    @DisplayName("POST /api/rides/{id}/start - Driver starts trip")
    void testDriverStartRideEndpoint() throws Exception {
        when(rideService.startRide(eq("ride-100"), eq("drv-acc-1")))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/rides/ride-100/start")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ride-100"));
    }

    // Cancel Ride Endpoint
    @Test
    @DisplayName("POST /api/rides/{id}/cancel - User cancels ride")
    void testCancelRideEndpoint() throws Exception {
        CancelRideRequest req = new CancelRideRequest("Driver took too long");
        when(rideService.cancelRide(eq("ride-100"), eq("pass-acc-1"), anyString(), eq("Driver took too long")))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/rides/ride-100/cancel")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ride-100"));
    }
}
