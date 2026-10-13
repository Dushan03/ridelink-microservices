package com.ridelink.account.dto;

import com.ridelink.account.model.Account;

import java.time.Instant;

/**
 * Safe account view returned to API consumers.
 * The password field is intentionally omitted.
 */
public record AccountResponse(
        String accountId,
        String name,
        String email,
        String phone,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    /** Factory method to convert an Account entity into a safe response. */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getName(),
                account.getEmail(),
                account.getPhone(),
                account.getRole().name(),
                account.getStatus().name(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
