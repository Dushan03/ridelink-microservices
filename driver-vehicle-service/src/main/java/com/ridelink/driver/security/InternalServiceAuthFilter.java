package com.ridelink.driver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.driver.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Filter that protects internal interservice endpoints (/api/internal/**)
 * by verifying the X-Internal-Service-Key header.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_INTERNAL_KEY = "X-Internal-Service-Key";

    @Value("${internal.service-key:ridelink-internal-secret-key-2026}")
    private String configuredInternalKey;

    private final ObjectMapper objectMapper;

    public InternalServiceAuthFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/internal/")) {
            String incomingKey = request.getHeader(HEADER_INTERNAL_KEY);

            if (incomingKey == null || incomingKey.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErrorResponse error = ErrorResponse.of(401, "UNAUTHORIZED",
                        "Missing internal service key header: " + HEADER_INTERNAL_KEY, path);
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            if (!configuredInternalKey.equals(incomingKey.trim())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErrorResponse error = ErrorResponse.of(403, "FORBIDDEN",
                        "Invalid internal service key", path);
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }

            // Set internal service principal
            var authToken = new UsernamePasswordAuthenticationToken(
                    "INTERNAL_SERVICE",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
