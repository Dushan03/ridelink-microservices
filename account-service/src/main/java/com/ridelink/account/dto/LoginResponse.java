package com.ridelink.account.dto;

/** JWT response returned after successful login. */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String accountId,
        String role
) {
    public static LoginResponse of(String token, long expiresInSeconds,
                                   String accountId, String role) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, accountId, role);
    }
}
