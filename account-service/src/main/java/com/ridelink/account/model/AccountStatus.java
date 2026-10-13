package com.ridelink.account.model;

/**
 * Lifecycle status of an Account.
 * ACTIVE  – can log in and use all services.
 * SUSPENDED – cannot log in; admin can reactivate.
 * DISABLED  – permanently deactivated.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DISABLED
}
