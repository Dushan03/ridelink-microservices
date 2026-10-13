package com.ridelink.account.controller;

import com.ridelink.account.dto.*;
import com.ridelink.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles public authentication endpoints: registration and login.
 * No JWT is required to call these routes.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public registration and login endpoints")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register/passenger")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a passenger account",
               responses = {
                   @ApiResponse(responseCode = "201", description = "Account created"),
                   @ApiResponse(responseCode = "400", description = "Validation error"),
                   @ApiResponse(responseCode = "409", description = "Email already registered")
               })
    public ResponseEntity<AccountResponse> registerPassenger(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.registerPassenger(request));
    }

    @PostMapping("/register/driver")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a driver account",
               responses = {
                   @ApiResponse(responseCode = "201", description = "Account created"),
                   @ApiResponse(responseCode = "400", description = "Validation error"),
                   @ApiResponse(responseCode = "409", description = "Email already registered")
               })
    public ResponseEntity<AccountResponse> registerDriver(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.registerDriver(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT",
               responses = {
                   @ApiResponse(responseCode = "200", description = "JWT issued"),
                   @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                   @ApiResponse(responseCode = "403", description = "Account suspended or disabled")
               })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }
}
