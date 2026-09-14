package com.example.ledger.service;

import com.example.ledger.config.AppProperties;
import com.example.ledger.domain.event.MoneyDeposited;
import com.example.ledger.domain.model.Account;
import com.example.ledger.eventstore.ConcurrencyException;
import com.example.ledger.eventstore.EventStore;
import com.example.ledger.eventstore.InMemoryEventStore;
import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.projection.BalanceProjection;
import com.example.ledger.snapshot.InMemorySnapshotStore;
import com.example.ledger.snapshot.SnapshotStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Event-sourcing behaviour tests. Everything is in-memory, so these run instantly with no
 * database, broker, or Docker.
 */
class AccountServiceTest {

    private AccountService newService(int snapshotEvery) {
        EventStore eventStore = new InMemoryEventStore();
        SnapshotStore snapshotStore = new InMemorySnapshotStore();
        BalanceProjection projection = new BalanceProjection();
        AppProperties props = new AppProperties(new AppProperties.Snapshot(snapshotEvery));
        return new AccountService(eventStore, snapshotStore, projection, props);
    }

    @Test
    void rebuildsBalanceFromEvents() {
        AccountService service = newService(0); // snapshots off
        Account acc = service.openAccount("Alice", new BigDecimal("100.00"));

        service.deposit(acc.id(), new BigDecimal("50.00"));
        service.withdraw(acc.id(), new BigDecimal("30.00"));

        Account reloaded = service.load(acc.id());
        assertThat(reloaded.balance()).isEqualByComparingTo("120.00");
        assertThat(reloaded.owner()).isEqualTo("Alice");
        // 3 events: opened, deposited(initial), deposited, withdrawn = version 4
        assertThat(reloaded.version()).isEqualTo(4L);
    }

    @Test
    void rejectsOverdraftAndAppendsNoEvent() {
        AccountService service = newService(0);
        Account acc = service.openAccount("Bob", new BigDecimal("40.00"));

        assertThatThrownBy(() -> service.withdraw(acc.id(), new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);

        // Balance unchanged and no withdrawal event was recorded.
        Account reloaded = service.load(acc.id());
        assertThat(reloaded.balance()).isEqualByComparingTo("40.00");
        assertThat(service.history(acc.id())).hasSize(2); // opened + initial deposit only
    }

    @Test
    void transferMovesFundsAtomically() {
        AccountService service = newService(0);
        Account from = service.openAccount("Src", new BigDecimal("100.00"));
        Account to = service.openAccount("Dst", BigDecimal.ZERO);

        service.transfer(from.id(), to.id(), new BigDecimal("60.00"));

        assertThat(service.load(from.id()).balance()).isEqualByComparingTo("40.00");
        assertThat(service.load(to.id()).balance()).isEqualByComparingTo("60.00");
    }

    @Test
    void replayAtVersionReturnsHistoricalState() {
        AccountService service = newService(0);
        Account acc = service.openAccount("Carol", new BigDecimal("100.00")); // v2 (opened, deposit)
        service.withdraw(acc.id(), new BigDecimal("25.00"));                   // v3
        service.deposit(acc.id(), new BigDecimal("10.00"));                    // v4

        // Time-travel: state right after the opening deposit (version 2) was 100.
        Account atV2 = service.stateAtVersion(acc.id(), 2);
        assertThat(atV2.balance()).isEqualByComparingTo("100.00");

        // Current state is 100 - 25 + 10 = 85.
        assertThat(service.load(acc.id()).balance()).isEqualByComparingTo("85.00");
    }

    @Test
    void snapshotThenLoadProducesSameState() {
        AccountService service = newService(1); // snapshot after every event
        Account acc = service.openAccount("Dave", new BigDecimal("10.00"));
        service.deposit(acc.id(), new BigDecimal("5.00"));
        service.deposit(acc.id(), new BigDecimal("5.00"));

        // Loading uses the latest snapshot + replay of any later events; must still be correct.
        Account reloaded = service.load(acc.id());
        assertThat(reloaded.balance()).isEqualByComparingTo("20.00");
    }

    @Test
    void optimisticConcurrencyConflictIsDetected() {
        EventStore store = new InMemoryEventStore();
        String id = "acc-1";

        // First append at version 0 succeeds; stream is now at version 1.
        store.append(id, 0, List.of(new MoneyDeposited(id, new BigDecimal("5.00"))));

        // A second append that still thinks the version is 0 must be rejected.
        assertThatThrownBy(() ->
                store.append(id, 0, List.of(new MoneyDeposited(id, new BigDecimal("5.00")))))
                .isInstanceOf(ConcurrencyException.class);
    }
}
