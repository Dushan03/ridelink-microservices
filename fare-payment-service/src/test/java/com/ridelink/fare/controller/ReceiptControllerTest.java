package com.ridelink.fare.controller;

import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.exception.GlobalExceptionHandler;
import com.ridelink.fare.exception.ResourceNotFoundException;
import com.ridelink.fare.security.JwtAuthFilter;
import com.ridelink.fare.service.ReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("GET /api/receipts/{receiptId} - 200 OK")
    void getReceiptById_Success_Returns200() throws Exception {
        ReceiptResponse response = new ReceiptResponse(
                "rcp-100", "RCP-XYZ-999", "pay-100", "fare-100", "ride-100", "passenger-1",
                new BigDecimal("450.00"), "LKR", "DIGITAL_WALLET", Instant.now()
        );

        when(receiptService.getReceiptById(eq("rcp-100"), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/receipts/rcp-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("rcp-100"))
                .andExpect(jsonPath("$.receiptNumber").value("RCP-XYZ-999"))
                .andExpect(jsonPath("$.amount").value(450.00));
    }

    @Test
    @WithMockUser(username = "passenger-1", roles = {"PASSENGER"})
    @DisplayName("GET /api/receipts/payment/{paymentId} - 404 Not Found")
    void getReceiptByPaymentId_NotFound_Returns404() throws Exception {
        when(receiptService.getReceiptByPaymentId(eq("pay-999"), any(), any()))
                .thenThrow(new ResourceNotFoundException("Receipt for payment", "pay-999"));

        mockMvc.perform(get("/api/receipts/payment/pay-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pay-999")));
    }
}
