package com.ridelink.ride.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional request body for POST /api/rides/{rideId}/cancel
 */
public record CancelRideRequest(
        @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
        String reason
) {}
