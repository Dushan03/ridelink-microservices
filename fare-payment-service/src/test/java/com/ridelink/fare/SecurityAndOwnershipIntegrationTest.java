package com.ridelink.fare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.entity.Fare;
import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.enums.FareType;
import com.ridelink.fare.enums.PaymentMethod;
import com.ridelink.fare.enums.PaymentStatus;
import com.ridelink.fare.repository.FareRepository;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;
import com.ridelink.fare.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb-security;DB_CLOSE_DELAY=-1",
    "jwt.secret=dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityAndOwnershipIntegrationTest {

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIVDI1Ng==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FareRepository fareRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    private String passenger1Id;
    private String passenger2Id;
    private String adminId;

    private String tokenPassenger1;
    private String tokenPassenger2;
    private String tokenAdmin;

    private Fare passenger1Fare;
    private Payment passenger1Payment;
    private Receipt passenger1Receipt;

    @BeforeEach
    void setUp() {
        receiptRepository.deleteAll();
        paymentRepository.deleteAll();
        fareRepository.deleteAll();

        passenger1Id = "passenger-uuid-111";
        passenger2Id = "passenger-uuid-222";
        adminId = "admin-uuid-999";

        tokenPassenger1 = jwtUtil.generateToken(passenger1Id, "p1@example.com", "PASSENGER");
        tokenPassenger2 = jwtUtil.generateToken(passenger2Id, "p2@example.com", "PASSENGER");
        tokenAdmin = jwtUtil.generateToken(adminId, "admin@example.com", "ADMIN");

        Instant now = Instant.now();
        passenger1Fare = Fare.builder()
                .id("fare-p1-001")
                .rideId("ride-p1-001")
                .passengerId(passenger1Id)
                .distanceKm(new BigDecimal("10.00"))
                .durationMinutes(new BigDecimal("20.00"))
                .baseFare(new BigDecimal("100.00"))
                .ratePerKm(new BigDecimal("50.00"))
                .ratePerMinute(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("640.00"))
                .currency("LKR")
                .fareType(FareType.FINAL)
                .createdAt(now)
                .finalizedAt(now)
                .build();
        fareRepository.save(passenger1Fare);

        passenger1Payment = Payment.builder()
                .id("pay-p1-001")
                .fareId(passenger1Fare.getId())
                .rideId(passenger1Fare.getRideId())
                .passengerId(passenger1Id)
                .amount(passenger1Fare.getTotalAmount())
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.SUCCESSFUL)
                .idempotencyKey("idem-p1-001")
                .createdAt(now)
                .processedAt(now)
                .build();
        paymentRepository.save(passenger1Payment);

        passenger1Receipt = Receipt.builder()
                .id("rcp-p1-001")
                .receiptNumber("RCP-TEST-001")
                .paymentId(passenger1Payment.getId())
                .fareId(passenger1Fare.getId())
                .rideId(passenger1Fare.getRideId())
                .passengerId(passenger1Id)
                .amount(passenger1Payment.getAmount())
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD)
                .issuedAt(now)
                .build();
        receiptRepository.save(passenger1Receipt);
    }

    @Test
    @DisplayName("17. Missing JWT returns 401 Unauthorized")
    void testMissingJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/fares/fare-p1-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("18. Invalid / Malformed JWT returns 401 Unauthorized")
    void testInvalidJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/fares/fare-p1-001")
                        .header("Authorization", "Bearer this-is-an-invalid-jwt-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("19. Expired JWT returns 401 Unauthorized")
    void testExpiredJwt_Returns401() throws Exception {
        // Create an already expired token
        Date past = new Date(System.currentTimeMillis() - 100000);
        String expiredToken = Jwts.builder()
                .subject(passenger1Id)
                .claim("email", "p1@example.com")
                .claim("role", "PASSENGER")
                .issuedAt(past)
                .expiration(past)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();

        mockMvc.perform(get("/api/fares/fare-p1-001")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("20. Passenger 2 accessing Passenger 1's Fare returns 403 Forbidden")
    void testPassengerOwnershipOnFare_Forbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/fares/fare-p1-001")
                        .header("Authorization", "Bearer " + tokenPassenger2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("20. Passenger 2 attempting to pay Passenger 1's Fare returns 403 Forbidden")
    void testPassengerOwnershipOnPayment_Forbidden_Returns403() throws Exception {
        // Create a new unpaid final fare for passenger 1
        Fare unpaidFare = Fare.builder()
                .id("fare-p1-unpaid")
                .rideId("ride-p1-unpaid")
                .passengerId(passenger1Id)
                .distanceKm(new BigDecimal("5.00"))
                .durationMinutes(new BigDecimal("10.00"))
                .baseFare(new BigDecimal("100.00"))
                .ratePerKm(new BigDecimal("50.00"))
                .ratePerMinute(new BigDecimal("2.00"))
                .totalAmount(new BigDecimal("370.00"))
                .currency("LKR")
                .fareType(FareType.FINAL)
                .createdAt(Instant.now())
                .finalizedAt(Instant.now())
                .build();
        fareRepository.save(unpaidFare);

        PaymentRequest request = new PaymentRequest(unpaidFare.getId(), PaymentMethod.CARD, false);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + tokenPassenger2)
                        .header("Idempotency-Key", "key-p2-attempt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("20. Passenger 2 accessing Passenger 1's Receipt returns 403 Forbidden")
    void testPassengerOwnershipOnReceipt_Forbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/receipts/rcp-p1-001")
                        .header("Authorization", "Bearer " + tokenPassenger2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("21. Administrator can view any passenger's Fare (200 OK)")
    void testAdminAccessToFare_Returns200() throws Exception {
        mockMvc.perform(get("/api/fares/fare-p1-001")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fareId").value("fare-p1-001"));
    }

    @Test
    @DisplayName("21. Administrator can view any passenger's Receipt (200 OK)")
    void testAdminAccessToReceipt_Returns200() throws Exception {
        mockMvc.perform(get("/api/receipts/rcp-p1-001")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("rcp-p1-001"));
    }
}
