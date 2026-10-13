package com.ridelink.driver.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.dto.CreateDriverProfileRequest;
import com.ridelink.driver.dto.ReserveDriverRequest;
import com.ridelink.driver.entity.DriverProfile;
import com.ridelink.driver.enums.AvailabilityStatus;
import com.ridelink.driver.repository.DriverProfileRepository;
import com.ridelink.driver.repository.VehicleRepository;
import com.ridelink.driver.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-driver-sec;DB_CLOSE_DELAY=-1",
    "jwt.secret=dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==",
    "internal.service-key=test-secret-key-123",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityAndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DriverProfileRepository driverProfileRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String driverToken;
    private String passengerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        driverProfileRepository.deleteAll();

        driverToken = jwtUtil.generateToken("acc-driver-1", "driver@test.com", "DRIVER");
        passengerToken = jwtUtil.generateToken("acc-passenger-1", "passenger@test.com", "PASSENGER");
        adminToken = jwtUtil.generateToken("acc-admin-1", "admin@test.com", "ADMIN");
    }

    @Test
    @DisplayName("Reject request to protected endpoint when Authorization header is missing (401)")
    void getMyProfile_MissingJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/drivers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Reject request when JWT token is invalid/tampered (401)")
    void getMyProfile_InvalidJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/drivers/me")
                        .header("Authorization", "Bearer invalid.fake.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reject driver operations when caller has PASSENGER role (403 Forbidden)")
    void createProfile_WithPassengerRole_Returns403() throws Exception {
        CreateDriverProfileRequest request = new CreateDriverProfileRequest(
                "LIC-9999",
                LocalDate.now().plusYears(2),
                "Colombo",
                6.9271,
                79.8612
        );

        mockMvc.perform(post("/api/drivers/profile")
                        .header("Authorization", "Bearer " + passengerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Reject internal reservation when X-Internal-Service-Key header is missing (401)")
    void reserveDriver_MissingInternalKey_Returns401() throws Exception {
        ReserveDriverRequest request = new ReserveDriverRequest("ride-123");

        mockMvc.perform(post("/api/internal/drivers/driver-1/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Reject internal reservation when X-Internal-Service-Key is incorrect (403)")
    void reserveDriver_IncorrectInternalKey_Returns403() throws Exception {
        ReserveDriverRequest request = new ReserveDriverRequest("ride-123");

        mockMvc.perform(post("/api/internal/drivers/driver-1/reserve")
                        .header("X-Internal-Service-Key", "wrong-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Allow internal reservation when valid X-Internal-Service-Key is supplied")
    void reserveDriver_ValidInternalKey_Success() throws Exception {
        DriverProfile profile = driverProfileRepository.save(DriverProfile.builder()
                .id("driver-1")
                .accountId("acc-driver-1")
                .licenseNumber("LIC-12345")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .serviceArea("Colombo")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build());

        ReserveDriverRequest request = new ReserveDriverRequest("ride-123");

        mockMvc.perform(post("/api/internal/drivers/" + profile.getId() + "/reserve")
                        .header("X-Internal-Service-Key", "test-secret-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatus").value("RESERVED"))
                .andExpect(jsonPath("$.currentRideId").value("ride-123"));
    }

    @Test
    @DisplayName("Admin can access get driver profile by ID (200)")
    void getDriverById_AdminRole_Returns200() throws Exception {
        DriverProfile profile = driverProfileRepository.save(DriverProfile.builder()
                .id("driver-admin-test")
                .accountId("acc-driver-admin")
                .licenseNumber("LIC-ADM")
                .licenseExpiryDate(LocalDate.now().plusYears(1))
                .serviceArea("Kandy")
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .build());

        mockMvc.perform(get("/api/drivers/" + profile.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("driver-admin-test"));
    }

    @Test
    @DisplayName("Return 400 Bad Request with fieldErrors map on validation failure")
    void createProfile_InvalidInput_Returns400WithFieldErrors() throws Exception {
        // Blank license and missing expiry date
        CreateDriverProfileRequest invalidRequest = new CreateDriverProfileRequest(
                "",
                null,
                "",
                999.0, // Invalid latitude
                999.0  // Invalid longitude
        );

        mockMvc.perform(post("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.fieldErrors.licenseNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.serviceArea").exists());
    }
}
