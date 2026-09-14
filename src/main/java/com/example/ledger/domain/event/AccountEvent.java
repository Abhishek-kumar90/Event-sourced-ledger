package com.example.ledger.domain.event;

/**
 * The domain events for an account. These are the <b>facts</b> that have happened — the
 * single source of truth in an event-sourced system. State is never stored directly;
 * it is always <i>derived</i> by folding these events in order.
 *
 * <p>The interface is {@code sealed}, so the set of event types is closed and known at
 * compile time. That lets the fold logic use an exhaustive {@code switch} with no
 * {@code default} branch — if someone adds a new event type, the compiler forces every
 * fold to handle it.</p>
 *
 * <p>Events carry only business facts. Metadata such as version, global sequence, and
 * timestamp lives on the {@link com.example.ledger.eventstore.StoredEvent} wrapper, not
 * here.</p>
 */
public sealed interface AccountEvent
        permits AccountOpened, MoneyDeposited, MoneyWithdrawn {

    /** The id of the account (event stream) this event belongs to. */
    String accountId();
}
