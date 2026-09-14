package com.example.ledger.projection;

import com.example.ledger.domain.event.AccountOpened;
import com.example.ledger.domain.event.MoneyDeposited;
import com.example.ledger.domain.event.MoneyWithdrawn;
import com.example.ledger.eventstore.StoredEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The balance read model (CQRS query side). It is updated by applying stored events as
 * they are appended, giving fast O(1) balance lookups without replaying a stream.
 *
 * <p>Because a projection is just a fold over events, it can be rebuilt at any time from
 * the event store — that's the whole appeal: the log is the truth, read models are
 * disposable.</p>
 */
@Component
public class BalanceProjection {

    private final Map<String, BalanceView> views = new ConcurrentHashMap<>();

    /** Apply a batch of newly stored events to the read model. */
    public void apply(List<StoredEvent> events) {
        for (StoredEvent e : events) {
            apply(e);
        }
    }

    private void apply(StoredEvent e) {
        switch (e.payload()) {
            case AccountOpened ev -> views.put(ev.accountId(),
                    new BalanceView(ev.accountId(), ev.owner(), BigDecimal.ZERO,
                            e.version(), e.recordedAt()));
            case MoneyDeposited ev -> update(ev.accountId(), ev.amount(), e);
            case MoneyWithdrawn ev -> update(ev.accountId(), ev.amount().negate(), e);
        }
    }

    private void update(String accountId, BigDecimal delta, StoredEvent e) {
        views.compute(accountId, (id, current) -> {
            BigDecimal base = current == null ? BigDecimal.ZERO : current.balance();
            String owner = current == null ? null : current.owner();
            return new BalanceView(accountId, owner, base.add(delta), e.version(), e.recordedAt());
        });
    }

    public Optional<BalanceView> get(String accountId) {
        return Optional.ofNullable(views.get(accountId));
    }

    public Collection<BalanceView> getAll() {
        return List.copyOf(views.values());
    }
}
