package com.ridelink.ride.client;

import com.ridelink.ride.dto.DriverEligibilityDto;
import com.ridelink.ride.dto.ReleaseDriverDto;
import com.ridelink.ride.dto.ReserveDriverDto;
import com.ridelink.ride.dto.TripActionDto;
import com.ridelink.ride.enums.VehicleType;
import com.ridelink.ride.exception.DriverReservationException;
import com.ridelink.ride.exception.InterserviceCommunicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * HTTP Client communicating with Driver & Vehicle Service (Port 8082).
 */
@Component
public class DriverServiceClient {

    public static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String driverServiceUrl;
    private final String internalKey;

    public DriverServiceClient(RestClient.Builder restClientBuilder,
                               @Value("${driver.service.url:http://localhost:8082}") String driverServiceUrl,
                               @Value("${internal.service-key:ridelink-internal-secret-key-2026}") String internalKey) {
        this.driverServiceUrl = driverServiceUrl;
        this.internalKey = internalKey;
        this.restClient = restClientBuilder
                .baseUrl(driverServiceUrl)
                .build();
    }

    /**
     * Calls GET /api/drivers/eligible to discover eligible available drivers.
     */
    public List<DriverEligibilityDto> findEligibleDrivers(String serviceArea,
                                                          VehicleType vehicleType,
                                                          Double pickupLatitude,
                                                          Double pickupLongitude,
                                                          Integer limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/drivers/eligible")
                            .queryParam("serviceArea", serviceArea)
                            .queryParam("vehicleType", vehicleType != null ? vehicleType.name() : null)
                            .queryParam("pickupLatitude", pickupLatitude)
                            .queryParam("pickupLongitude", pickupLongitude)
                            .queryParam("limit", limit != null ? limit : 10)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DriverEligibilityDto>>() {});
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Driver Service", "Service is unavailable or connection timed out", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Driver Service", "Driver eligibility search failed: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Driver Service", "Unexpected error during driver lookup: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Calls POST /api/internal/drivers/{driverId}/reserve to atomically reserve the driver.
     */
    public void reserveDriver(String driverId, String rideId) {
        try {
            restClient.post()
                    .uri("/api/internal/drivers/{driverId}/reserve", driverId)
                    .header(HEADER_INTERNAL_KEY, internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReserveDriverDto(rideId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw new DriverReservationException("Driver " + driverId + " is no longer available or was reserved by another ride");
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Driver Service", "Service is unreachable during reservation", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Driver Service", "Failed to reserve driver: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Driver Service", "Unexpected reservation failure: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Calls POST /api/internal/drivers/{driverId}/start-trip to transition driver to ON_TRIP.
     */
    public void startTrip(String driverId, String rideId) {
        try {
            restClient.post()
                    .uri("/api/internal/drivers/{driverId}/start-trip", driverId)
                    .header(HEADER_INTERNAL_KEY, internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TripActionDto(rideId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Driver Service", "Service is unreachable during trip start", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Driver Service", "Failed to start trip: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Driver Service", "Unexpected trip start failure: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Calls POST /api/internal/drivers/{driverId}/release to release driver back to AVAILABLE.
     */
    public void releaseDriver(String driverId, String rideId, String reason) {
        try {
            restClient.post()
                    .uri("/api/internal/drivers/{driverId}/release", driverId)
                    .header(HEADER_INTERNAL_KEY, internalKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReleaseDriverDto(rideId, reason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new InterserviceCommunicationException("Driver Service", "Service is unreachable during driver release", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InterserviceCommunicationException("Driver Service", "Failed to release driver: " + e.getResponseBodyAsString(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            throw new InterserviceCommunicationException("Driver Service", "Unexpected driver release failure: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
}
