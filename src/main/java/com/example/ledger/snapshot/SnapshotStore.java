package com.example.ledger.snapshot;

import com.example.ledger.domain.model.Snapshot;

import java.util.Optional;

/**
 * Stores the latest snapshot per account. Like the event store, it sits behind an
 * interface so a real backend could replace the in-memory version later.
 */
public interface SnapshotStore {

    /** Save (overwrite) the latest snapshot for an account. */
    void save(Snapshot snapshot);

    /** The latest snapshot for an account, if any. */
    Optional<Snapshot> load(String accountId);
}
