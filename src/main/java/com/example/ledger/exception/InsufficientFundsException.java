package com.example.ledger.exception;

/**
 * Thrown at command time when a withdrawal or transfer would overdraw an account. Because
 * this is checked BEFORE any event is emitted, an invalid command never reaches the log.
 * Maps to HTTP 422.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountId) {
        super("Insufficient funds in account: " + accountId);
    }
}
