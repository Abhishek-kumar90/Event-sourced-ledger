package com.example.ledger.eventstore;

/**
 * Thrown when an append's expected version does not match the stream's current version —
 * i.e. someone else modified the account since we loaded it. This is the optimistic
 * concurrency check that keeps the event log consistent. Maps to HTTP 409 Conflict.
 */
public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String streamId, long expected, long actual) {
        super("Concurrent modification of account '" + streamId
                + "': expected version " + expected + " but was " + actual + ".");
    }
}
