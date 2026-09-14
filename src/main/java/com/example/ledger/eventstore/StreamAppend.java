package com.example.ledger.eventstore;

import com.example.ledger.domain.event.AccountEvent;

import java.util.List;

/**
 * One stream's part of an atomic multi-stream append. A transfer, for example, is two
 * of these (debit one account, credit another) applied all-or-nothing.
 *
 * @param streamId        the account id
 * @param expectedVersion the version the caller believes the stream is at (optimistic lock)
 * @param events          the events to append to this stream
 */
public record StreamAppend(String streamId, long expectedVersion, List<AccountEvent> events) {
}
