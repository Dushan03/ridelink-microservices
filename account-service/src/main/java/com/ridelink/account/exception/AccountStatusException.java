package com.ridelink.account.exception;

import com.ridelink.account.model.AccountStatus;

/** Thrown when a suspended or disabled account attempts to log in. */
public class AccountStatusException extends RuntimeException {
    private final AccountStatus status;

    public AccountStatusException(AccountStatus status) {
        super("Account is " + status.name().toLowerCase());
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
