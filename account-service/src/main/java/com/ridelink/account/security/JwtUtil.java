package com.ridelink.account.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class responsible for:
 * <ul>
 *   <li>Generating signed JWTs after successful login.</li>
 *   <li>Validating and parsing JWTs on every secured request.</li>
 * </ul>
 *
 * <p>The signing secret is read from the {@code jwt.secret} Spring property,
 * which is itself resolved from the {@code JWT_SECRET} environment variable
 * with a safe dev-only fallback (see application.properties).
 *
 * <p>Token format (payload):
 * <pre>
 * { "sub": "<accountId>", "email": "<email>", "role": "<role>",
 *   "iat": <epoch>, "exp": <epoch> }
 * </pre>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Creates a signed JWT for the given account.
     *
     * @param accountId UUID string of the account (becomes the "sub" claim)
     * @param email     user email (informational claim)
     * @param role      role string, e.g. "PASSENGER"
     * @return compact JWT string
     */
    public String generateToken(String accountId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(accountId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts all claims from a token. Throws JwtException if invalid/expired.
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractAccountId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /** Returns {@code true} if the token is parseable and not expired. */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Returns the expiration duration in seconds (for the LoginResponse). */
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
