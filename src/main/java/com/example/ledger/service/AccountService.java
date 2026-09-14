package com.example.ledger.service;

import com.example.ledger.config.AppProperties;
import com.example.ledger.domain.event.AccountEvent;
import com.example.ledger.domain.event.AccountOpened;
import com.example.ledger.domain.event.MoneyDeposited;
import com.example.ledger.domain.event.MoneyWithdrawn;
import com.example.ledger.domain.model.Account;
import com.example.ledger.eventstore.EventStore;
import com.example.ledger.eventstore.StoredEvent;
import com.example.ledger.eventstore.StreamAppend;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.exception.InvalidCommandException;
import com.example.ledger.projection.BalanceProjection;
import com.example.ledger.snapshot.SnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The application service. It contains the <b>command handlers</b>: each public method
 * validates business rules, and only if they pass does it emit events to the store. This
 * is the one place where an operation can be rejected — the fold that rebuilds state
 * never rejects anything.
 *
 * <p>Loading an account = latest snapshot (if any) + replay of the events after it. That
 * derived {@link Account} is the source of truth; the {@link BalanceProjection} is a fast
 * read model kept in sync as events are appended.</p>
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    private final BalanceProjection projection;
    private final int snapshotEvery;

    public AccountService(EventStore eventStore,
                          SnapshotStore snapshotStore,
                          BalanceProjection projection,
                          AppProperties props) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.projection = projection;
        this.snapshotEvery = props.snapshot().every();
    }

    // ---------------------------------------------------------------------------------
    // Command handlers (write side): validate, then emit events.
    // ---------------------------------------------------------------------------------

    /** Open a new account, optionally with an initial deposit. */
    public Account openAccount(String owner, BigDecimal initialDeposit) {
        if (owner == null || owner.isBlank()) {
            throw new InvalidCommandException("owner is required");
        }
        String accountId = UUID.randomUUID().toString();

        List<AccountEvent> events = new java.util.ArrayList<>();
        events.add(new AccountOpened(accountId, owner));
        if (initialDeposit != null && initialDeposit.signum() > 0) {
            events.add(new MoneyDeposited(accountId, initialDeposit));
        } else if (initialDeposit != null && initialDeposit.signum() < 0) {
            throw new InvalidCommandException("initialDeposit cannot be negative");
        }

        // expectedVersion 0: this stream must not already exist.
        List<StoredEvent> stored = eventStore.append(accountId, 0, events);
        projection.apply(stored);
        maybeSnapshot(accountId);
        log.info("Opened account {} (version {})", accountId, stored.getLast().version());
        return load(accountId);
    }

    /** Deposit money into an account. */
    public Account deposit(String accountId, BigDecimal amount) {
        requirePositive(amount, "deposit amount");
        Account account = load(accountId);
        List<StoredEvent> stored = eventStore.append(accountId, account.version(),
                List.of(new MoneyDeposited(accountId, amount)));
        projection.apply(stored);
        maybeSnapshot(accountId);
        return load(accountId);
    }

    /** Withdraw money, rejecting the command if it would overdraw the account. */
    public Account withdraw(String accountId, BigDecimal amount) {
        requirePositive(amount, "withdrawal amount");
        Account account = load(accountId);
        if (account.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountId);
        }
        List<StoredEvent> stored = eventStore.append(accountId, account.version(),
                List.of(new MoneyWithdrawn(accountId, amount)));
        projection.apply(stored);
        maybeSnapshot(accountId);
        return load(accountId);
    }

    /**
     * Transfer money between two accounts atomically. The debit and credit are appended
     * to their two streams all-or-nothing, so money is never lost or created even if one
     * side would fail an optimistic-concurrency check.
     */
    public void transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        requirePositive(amount, "transfer amount");
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidCommandException("cannot transfer to the same account");
        }
        Account from = load(fromAccountId);
        Account to = load(toAccountId);
        if (from.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromAccountId);
        }

        List<StoredEvent> stored = eventStore.appendAtomic(List.of(
                new StreamAppend(fromAccountId, from.version(),
                        List.of(new MoneyWithdrawn(fromAccountId, amount))),
                new StreamAppend(toAccountId, to.version(),
                        List.of(new MoneyDeposited(toAccountId, amount)))
        ));
        projection.apply(stored);
        maybeSnapshot(fromAccountId);
        maybeSnapshot(toAccountId);
        log.info("Transferred {} from {} to {}", amount, fromAccountId, toAccountId);
    }

    // ---------------------------------------------------------------------------------
    // Query side (rebuild from events).
    // ---------------------------------------------------------------------------------

    /** Load current state: latest snapshot + replay of events after it. */
    public Account load(String accountId) {
        Optional<com.example.ledger.domain.model.Snapshot> snapshot = snapshotStore.load(accountId);
        boolean exists = eventStore.exists(accountId);
        if (snapshot.isEmpty() && !exists) {
            throw new AccountNotFoundException(accountId);
        }

        Account account = snapshot.map(Account::fromSnapshot).orElseGet(() -> Account.empty(accountId));
        long from = snapshot.map(com.example.ledger.domain.model.Snapshot::version).orElse(0L);

        for (StoredEvent stored : eventStore.readStream(accountId)) {
            if (stored.version() > from) {
                account = account.apply(stored.payload());
            }
        }
        return account;
    }

    /** Rebuild state as of a specific version (time-travel by version). */
    public Account stateAtVersion(String accountId, long version) {
        List<StoredEvent> events = eventStore.readStreamUpToVersion(accountId, version);
        if (events.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        Account account = Account.empty(accountId);
        for (StoredEvent stored : events) {
            account = account.apply(stored.payload());
        }
        return account;
    }

    /** Rebuild state as of a point in time (time-travel by timestamp). */
    public Account stateAsOf(String accountId, Instant asOf) {
        List<StoredEvent> events = eventStore.readStreamUpToTime(accountId, asOf);
        if (events.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        Account account = Account.empty(accountId);
        for (StoredEvent stored : events) {
            account = account.apply(stored.payload());
        }
        return account;
    }

    /** The full event history of an account (the audit trail). */
    public List<StoredEvent> history(String accountId) {
        if (!eventStore.exists(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return eventStore.readStream(accountId);
    }

    // ---------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------

    private void maybeSnapshot(String accountId) {
        if (snapshotEvery <= 0) {
            return;
        }
        Account account = load(accountId);
        if (account.version() % snapshotEvery == 0) {
            snapshotStore.save(account.toSnapshot());
            log.debug("Snapshot saved for {} at version {}", accountId, account.version());
        }
    }

    private void requirePositive(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidCommandException(label + " must be positive");
        }
    }
}
