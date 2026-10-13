package com.ridelink.account.dto;

import com.ridelink.account.model.Role;
import jakarta.validation.constraints.*;

/**
 * Incoming request body for both passenger and driver registration.
 * The role is determined by the endpoint path, not by this DTO.
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must be 150 characters or fewer")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "Password must contain at least one uppercase letter, one digit and one special character"
        )
        String password,

        @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Phone must be a valid international phone number"
        )
        String phone
) {}
