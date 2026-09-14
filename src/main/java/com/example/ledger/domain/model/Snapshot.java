package com.example.ledger.domain.model;

import java.math.BigDecimal;

/**
 * A point-in-time snapshot of an account's derived state, taken every N events. Loading
 * an account can then start from the latest snapshot and replay only the events that
 * came after it, instead of replaying the entire history from event 1.
 *
 * @param accountId the account
 * @param owner     owner at snapshot time
 * @param balance   balance at snapshot time
 * @param version   the event version this snapshot represents
 */
public record Snapshot(String accountId, String owner, BigDecimal balance, long version) {
}
