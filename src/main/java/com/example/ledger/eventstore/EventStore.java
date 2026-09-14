package com.example.ledger.eventstore;

import com.example.ledger.domain.event.AccountEvent;

import java.time.Instant;
import java.util.List;

/**
 * The append-only event store. This interface is the seam that keeps the domain free of
 * infrastructure: today it is backed by memory, but a PostgreSQL- or Kafka-backed
 * implementation could replace {@link InMemoryEventStore} without any change to the
 * service or domain code.
 *
 * <p>Two invariants define an event store:</p>
 * <ul>
 *   <li>events are only ever appended, never updated or deleted; and</li>
 *   <li>appends are guarded by an expected version (optimistic concurrency).</li>
 * </ul>
 */
public interface EventStore {

    /**
     * Append events to a single stream.
     *
     * @param streamId        the account id
     * @param expectedVersion the version the caller last saw (0 for a brand-new stream)
     * @param events          events to append, in order
     * @return the stored events, with assigned versions/sequence/timestamps
     * @throws ConcurrencyException if {@code expectedVersion} != the stream's current version
     */
    List<StoredEvent> append(String streamId, long expectedVersion, List<AccountEvent> events);

    /**
     * Append to several streams atomically: either every stream's events are appended, or
     * none are. Used for transfers so money can never be debited without being credited.
     *
     * @throws ConcurrencyException if any stream's expected version does not match
     */
    List<StoredEvent> appendAtomic(List<StreamAppend> appends);

    /** All events for a stream, in version order. Empty if the stream does not exist. */
    List<StoredEvent> readStream(String streamId);

    /** Events for a stream up to and including the given version (for replay to a point). */
    List<StoredEvent> readStreamUpToVersion(String streamId, long version);

    /** Events for a stream recorded at or before the given instant (time-travel). */
    List<StoredEvent> readStreamUpToTime(String streamId, Instant asOf);

    /** Every event in the store, in global sequence order. */
    List<StoredEvent> readAll();

    /** True if the stream has at least one event. */
    boolean exists(String streamId);
}
