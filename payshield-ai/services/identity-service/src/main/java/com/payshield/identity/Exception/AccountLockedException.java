package com.payshield.identity.Exception;

public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
        super("Account is temporarily unavailable");
    }
}
