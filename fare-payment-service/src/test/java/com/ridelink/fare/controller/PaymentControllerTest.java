package com.ridelink.fare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.enums.PaymentMethod;
import com.ridelink.fare.exception.GlobalExceptionHandler;
import com.ridelink.fare.exception.PaymentConflictException;
import com.ridelink.fare.security.JwtAuthFilter;
import com.ridelink.fare.service.PaymentService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("POST /api/payments - 200 OK with valid payment")
    void processPayment_Success_Returns200() throws Exception {
        PaymentRequest request = new PaymentRequest("fare-1", PaymentMethod.CARD, false);

        ReceiptResponse receipt = new ReceiptResponse(
                "rcp-1", "RCP-ABC-123", "pay-1", "fare-1", "ride-1", "passenger-1",
                new BigDecimal("500.00"), "LKR", "CARD", Instant.now()
        );
        PaymentResponse response = new PaymentResponse(
                "pay-1", "fare-1", "ride-1", "passenger-1",
                new BigDecimal("500.00"), "LKR", "CARD", "SUCCESSFUL",
                "idem-key-1", null, receipt, Instant.now(), Instant.now()
        );

        when(paymentService.processPayment(any(PaymentRequest.class), eq("idem-key-1"), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("pay-1"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.receipt.receiptNumber").value("RCP-ABC-123"));
    }

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("POST /api/payments - 400 Bad Request on missing Idempotency-Key header")
    void processPayment_MissingIdempotencyHeader_Returns400() throws Exception {
        PaymentRequest request = new PaymentRequest("fare-1", PaymentMethod.CARD, false);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_HEADER"));
    }

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("POST /api/payments - 409 Conflict when paying already settled fare")
    void processPayment_AlreadyPaidFare_Returns409() throws Exception {
        PaymentRequest request = new PaymentRequest("fare-1", PaymentMethod.CARD, false);

        when(paymentService.processPayment(any(PaymentRequest.class), eq("idem-key-1"), any(), any()))
                .thenThrow(new PaymentConflictException("Fare fare-1 has already been successfully paid"));

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAYMENT_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("GET /api/payments/my - 200 OK")
    void getMyPayments_Success_Returns200() throws Exception {
        PaymentResponse response = new PaymentResponse(
                "pay-1", "fare-1", "ride-1", "passenger-1",
                new BigDecimal("500.00"), "LKR", "CARD", "SUCCESSFUL",
                "idem-key-1", null, null, Instant.now(), Instant.now()
        );

        when(paymentService.getMyPayments(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paymentId").value("pay-1"));
    }
}
