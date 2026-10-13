package com.ridelink.ride.client;

import com.ridelink.ride.dto.FareEstimateRequestDto;
import com.ridelink.ride.dto.FareFinalRequestDto;
import com.ridelink.ride.dto.FareResponseDto;
import com.ridelink.ride.exception.InterserviceCommunicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * HTTP Client communicating with Fare & Payment Service (Port 8084).
 */
@Component
public class FareServiceClient {

    private final RestClient restClient;
    private final String fareServiceUrl;

    public FareServiceClient(RestClient.Builder restClientBuilder,
                             @Value("${fare.service.url:http://localhost:8084}") String fareServiceUrl) {
        this.fareServiceUrl = fareServiceUrl;
        this.restClient = restClientBuilder
                .baseUrl(fareServiceUrl)
                .build();
    }

    /**
     * Calls POST /api/fares/estimate to calculate an indicative fare estimate.
     */
    public FareResponseDto calculateEstimate(BigDecimal distanceKm, BigDecimal durationMinutes) {
        try {
            return restClient.post()
                    .uri("/api/fares/estimate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FareEstimateRequestDto(distanceKm, durationMinutes))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FareResponseDto.class);
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Fare Service", "Service is unreachable during fare estimation", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Fare Service", "Fare estimation calculation failed: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Fare Service", "Unexpected fare estimation error: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Calls POST /api/fares/final upon ride completion to generate the binding final fare.
     */
    public FareResponseDto createFinalFare(String rideId, String passengerId, BigDecimal distanceKm, BigDecimal durationMinutes, String token) {
        try {
            var requestSpec = restClient.post()
                    .uri("/api/fares/final")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new FareFinalRequestDto(rideId, passengerId, distanceKm, durationMinutes))
                    .accept(MediaType.APPLICATION_JSON);

            if (token != null && !token.isBlank()) {
                requestSpec.header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }

            return requestSpec.retrieve().body(FareResponseDto.class);
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Fare Service", "Service is unreachable during final fare calculation", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Fare Service", "Final fare calculation failed: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Fare Service", "Unexpected final fare calculation error: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
}
