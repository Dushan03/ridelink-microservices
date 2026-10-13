package com.ridelink.account.controller;

import com.ridelink.account.dto.*;
import com.ridelink.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Secured account management endpoints.
 *
 * <p>The authenticated principal is the accountId extracted from the JWT by JwtAuthFilter.
 * Ownership check: users can only access their own account via /me endpoints.
 * ADMIN can change any account's status.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Secured account profile and admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get own profile (PASSENGER, DRIVER, ADMIN)")
    public ResponseEntity<AccountResponse> getMe(Authentication auth) {
        // auth.getName() returns the accountId set by JwtAuthFilter
        return ResponseEntity.ok(accountService.getAccountById(auth.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update own profile – name and phone only (PASSENGER, DRIVER)")
    @PreAuthorize("hasAnyRole('PASSENGER','DRIVER')")
    public ResponseEntity<AccountResponse> updateMe(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(auth.getName(), request));
    }

    @PatchMapping("/{accountId}/status")
    @Operation(summary = "Change account status (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable String accountId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(accountService.updateStatus(accountId, request));
    }
}
