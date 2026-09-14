package com.example.ledger.projection;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A row in the read model: the current balance of one account, optimised for fast reads.
 * This is the "query" side of CQRS — it is derived from events and can always be thrown
 * away and rebuilt by replaying the log.
 *
 * @param accountId   the account
 * @param owner       owner name
 * @param balance     current balance
 * @param version     the event version this view reflects
 * @param lastUpdated when the view last changed
 */
public record BalanceView(
        String accountId,
        String owner,
        BigDecimal balance,
        long version,
        Instant lastUpdated
) {
}
