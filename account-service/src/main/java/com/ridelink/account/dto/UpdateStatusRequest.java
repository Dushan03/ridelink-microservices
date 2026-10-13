package com.ridelink.account.dto;

import com.ridelink.account.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

/** Admin request to change an account's status. */
public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        AccountStatus status,

        String reason
) {}
