package com.ridelink.account.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Fields the user may change on their own profile (name and/or phone). */
public record UpdateProfileRequest(
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Phone must be a valid international phone number"
        )
        String phone
) {}
