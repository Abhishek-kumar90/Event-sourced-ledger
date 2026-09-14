package com.example.ledger.domain.model;

import com.example.ledger.domain.event.AccountEvent;
import com.example.ledger.domain.event.AccountOpened;
import com.example.ledger.domain.event.MoneyDeposited;
import com.example.ledger.domain.event.MoneyWithdrawn;

import java.math.BigDecimal;

/**
 * The Account aggregate — the current state of one account, always <b>derived</b> by
 * folding its event stream. It is immutable: {@link #apply(AccountEvent)} returns a new
 * {@code Account} rather than mutating in place, which mirrors the append-only nature of
 * the event log.
 *
 * <p>Important event-sourcing principle: applying an event NEVER fails. Events are facts
 * that already happened, so the fold is total. All business validation (e.g. "cannot
 * overdraw") happens at <i>command</i> time, before an event is ever emitted — see
 * {@code AccountService}. That keeps replay deterministic and safe.</p>
 *
 * @param id      the account id
 * @param owner   the owner's name (null until AccountOpened has been applied)
 * @param balance the current balance
 * @param version the number of events applied so far (matches the event store version)
 */
public record Account(String id, String owner, BigDecimal balance, long version) {

    /** An empty account with no events applied yet (version 0, zero balance). */
    public static Account empty(String id) {
        return new Account(id, null, BigDecimal.ZERO, 0L);
    }

    /** Reconstruct an account starting point from a snapshot. */
    public static Account fromSnapshot(Snapshot snapshot) {
        return new Account(snapshot.accountId(), snapshot.owner(),
                snapshot.balance(), snapshot.version());
    }

    /**
     * Fold one event into state, returning the next {@code Account}. Exhaustive over the
     * sealed event type — no {@code default} needed, so a new event type is a compile
     * error until handled here.
     */
    public Account apply(AccountEvent event) {
        return switch (event) {
            case AccountOpened e -> new Account(e.accountId(), e.owner(), BigDecimal.ZERO, version + 1);
            case MoneyDeposited e -> new Account(id, owner, balance.add(e.amount()), version + 1);
            case MoneyWithdrawn e -> new Account(id, owner, balance.subtract(e.amount()), version + 1);
        };
    }

    /** Capture the current state as a snapshot. */
    public Snapshot toSnapshot() {
        return new Snapshot(id, owner, balance, version);
    }

    /** True once the opening event has been applied. */
    public boolean isOpened() {
        return owner != null;
    }
}
