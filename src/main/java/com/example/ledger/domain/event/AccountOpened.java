package com.example.ledger.domain.event;

/**
 * Recorded when a new account is opened. Always the first event in an account's stream.
 *
 * @param accountId the new account's id
 * @param owner     the account owner's name
 */
public record AccountOpened(String accountId, String owner) implements AccountEvent {
}
