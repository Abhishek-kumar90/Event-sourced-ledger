package com.example.ledger.exception;

/** Thrown for invalid commands (non-positive amount, transfer to self). Maps to HTTP 400. */
public class InvalidCommandException extends RuntimeException {
    public InvalidCommandException(String message) {
        super(message);
    }
}
