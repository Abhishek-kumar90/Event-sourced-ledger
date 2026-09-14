package com.example.ledger.snapshot;

import com.example.ledger.domain.model.Snapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory snapshot store: one latest snapshot per account. Newer snapshots overwrite
 * older ones, since only the most recent is needed to speed up a rebuild.
 */
@Component
public class InMemorySnapshotStore implements SnapshotStore {

    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void save(Snapshot snapshot) {
        snapshots.put(snapshot.accountId(), snapshot);
    }

    @Override
    public Optional<Snapshot> load(String accountId) {
        return Optional.ofNullable(snapshots.get(accountId));
    }
}
