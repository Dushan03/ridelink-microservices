package com.ridelink.account.service;

import com.ridelink.account.dto.*;
import com.ridelink.account.exception.*;
import com.ridelink.account.model.*;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Core business logic for account management.
 *
 * <p>Constructor injection is used throughout (SOLID: Dependency Inversion Principle).
 * Business rules are kept here, not in the controller or repository.
 */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtUtil           = jwtUtil;
    }

    /**
     * Registers a new passenger account.
     * Throws DuplicateEmailException if email is already taken.
     */
    public AccountResponse registerPassenger(RegisterRequest request) {
        return register(request, Role.PASSENGER);
    }

    /**
     * Registers a new driver account (account only; operational profile is in Driver Service).
     */
    public AccountResponse registerDriver(RegisterRequest request) {
        return register(request, Role.DRIVER);
    }

    /**
     * Authenticates a user and returns a JWT on success.
     * Throws InvalidCredentialsException on wrong email/password.
     * Throws AccountStatusException for suspended/disabled accounts.
     */
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountStatusException(account.getStatus());
        }

        String token = jwtUtil.generateToken(
                account.getAccountId(),
                account.getEmail(),
                account.getRole().name()
        );

        return LoginResponse.of(token, jwtUtil.getExpirationSeconds(),
                account.getAccountId(), account.getRole().name());
    }

    /**
     * Returns the profile for the account with the given accountId.
     * The accountId comes from the validated JWT subject.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String accountId) {
        Account account = findOrThrow(accountId);
        return AccountResponse.from(account);
    }

    /**
     * Updates mutable fields (name, phone) for the given account.
     */
    public AccountResponse updateProfile(String accountId, UpdateProfileRequest request) {
        Account account = findOrThrow(accountId);

        if (request.name() != null && !request.name().isBlank()) {
            account.setName(request.name().trim());
        }
        if (request.phone() != null) {
            account.setPhone(request.phone().isBlank() ? null : request.phone());
        }

        return AccountResponse.from(accountRepository.save(account));
    }

    /**
     * Admin operation: change the status of any account.
     */
    public AccountResponse updateStatus(String accountId, UpdateStatusRequest request) {
        Account account = findOrThrow(accountId);
        account.setStatus(request.status());
        return AccountResponse.from(accountRepository.save(account));
    }

    // ──────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────

    private AccountResponse register(RegisterRequest request, Role role) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Account account = Account.builder()
                .accountId(UUID.randomUUID().toString())
                .name(request.name().trim())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(role)
                .status(AccountStatus.ACTIVE)
                .build();

        return AccountResponse.from(accountRepository.save(account));
    }

    private Account findOrThrow(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }
}
