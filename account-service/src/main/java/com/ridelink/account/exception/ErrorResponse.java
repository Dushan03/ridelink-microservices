package com.ridelink.account.exception;

import java.time.Instant;

/**
 * Standardised JSON error body returned by every endpoint on failure.
 * Matches the contract in docs/api-contracts.md.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path);
    }
}
