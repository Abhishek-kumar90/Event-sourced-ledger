package com.example.ledger.exception;

/** Thrown when an operation references an account that has no events. Maps to HTTP 404. */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
