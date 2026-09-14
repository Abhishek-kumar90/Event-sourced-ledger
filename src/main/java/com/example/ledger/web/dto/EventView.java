package com.example.ledger.web.dto;

import com.example.ledger.domain.event.MoneyDeposited;
import com.example.ledger.domain.event.MoneyWithdrawn;
import com.example.ledger.eventstore.StoredEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One entry in an account's history (the audit trail). Amount is null for events that
 * have no monetary amount (e.g. AccountOpened).
 *
 * @param globalSequence store-wide sequence number
 * @param version        per-account version
 * @param type           event type name
 * @param amount         monetary amount, if applicable
 * @param recordedAt     when it happened
 */
public record EventView(
        long globalSequence,
        long version,
        String type,
        BigDecimal amount,
        Instant recordedAt
) {
    public static EventView from(StoredEvent e) {
        BigDecimal amount = switch (e.payload()) {
            case MoneyDeposited d -> d.amount();
            case MoneyWithdrawn w -> w.amount();
            default -> null;
        };
        return new EventView(e.globalSequence(), e.version(), e.eventType(), amount, e.recordedAt());
    }
}
