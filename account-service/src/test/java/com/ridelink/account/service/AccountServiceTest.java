package com.ridelink.account.service;

import com.ridelink.account.dto.*;
import com.ridelink.account.exception.*;
import com.ridelink.account.model.Account;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.repository.AccountRepository;
import com.ridelink.account.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AccountService accountService;

    private RegisterRequest registerRequest;
    private Account account;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "Password123!",
                "+94771234567"
        );

        account = Account.builder()
                .accountId("test-account-id")
                .name("John Doe")
                .email("john@example.com")
                .password("hashed-pw")
                .phone("+94771234567")
                .role(Role.PASSENGER)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Successfully register passenger")
    void registerPassenger_Success() {
        when(accountRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pw");
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.registerPassenger(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo("PASSENGER");
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Fail registration when email is already registered")
    void register_DuplicateEmail_ThrowsException() {
        when(accountRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> accountService.registerPassenger(registerRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already registered: john@example.com");

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Successfully login with correct credentials")
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "Password123!");

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123!", "hashed-pw")).thenReturn(true);
        when(jwtUtil.generateToken("test-account-id", "john@example.com", "PASSENGER")).thenReturn("mock-jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(86400L);

        LoginResponse response = accountService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock-jwt-token");
        assertThat(response.accountId()).isEqualTo("test-account-id");
        assertThat(response.role()).isEqualTo("PASSENGER");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Fail login with incorrect password")
    void login_InvalidPassword_ThrowsException() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "WrongPassword!");

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPassword!", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> accountService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Fail login when account is SUSPENDED")
    void login_SuspendedAccount_ThrowsException() {
        account.setStatus(AccountStatus.SUSPENDED);
        LoginRequest loginRequest = new LoginRequest("john@example.com", "Password123!");

        when(accountRepository.findByEmail("john@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123!", "hashed-pw")).thenReturn(true);

        assertThatThrownBy(() -> accountService.login(loginRequest))
                .isInstanceOf(AccountStatusException.class)
                .hasMessageContaining("Account is suspended");
    }

    @Test
    @DisplayName("Successfully update profile")
    void updateProfile_Success() {
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("John Updated", "+94779999999");
        when(accountRepository.findById("test-account-id")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateProfile("test-account-id", updateRequest);

        assertThat(response).isNotNull();
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("Successfully update account status by admin")
    void updateAccountStatus_Success() {
        UpdateStatusRequest statusRequest = new UpdateStatusRequest(AccountStatus.SUSPENDED, "Violation of terms");
        when(accountRepository.findById("test-account-id")).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        AccountResponse response = accountService.updateStatus("test-account-id", statusRequest);

        assertThat(response).isNotNull();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }
}
