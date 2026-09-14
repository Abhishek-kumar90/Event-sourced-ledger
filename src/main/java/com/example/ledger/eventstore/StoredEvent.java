package com.example.ledger.eventstore;

import com.example.ledger.domain.event.AccountEvent;

import java.time.Instant;

/**
 * An event as persisted in the store: the business event plus its metadata.
 *
 * @param globalSequence a store-wide, monotonically increasing sequence number
 * @param streamId       the account id this event belongs to
 * @param version        the per-stream version (1-based, gap-free)
 * @param eventType      simple class name of the event, handy for logs and read models
 * @param payload        the actual domain event
 * @param recordedAt     when the event was appended (used for time-travel queries)
 */
public record StoredEvent(
        long globalSequence,
        String streamId,
        long version,
        String eventType,
        AccountEvent payload,
        Instant recordedAt
) {
}
