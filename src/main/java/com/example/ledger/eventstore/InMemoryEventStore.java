package com.example.ledger.eventstore;

import com.example.ledger.domain.event.AccountEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory event store. No database or Docker required — everything lives in memory and
 * is lost on restart, which is perfect for local development and demos.
 *
 * <p>Concurrency model: one {@link ReentrantLock} per stream. Single-stream appends lock
 * that stream; multi-stream appends lock every involved stream <b>in a consistent order
 * (sorted by id)</b> to avoid deadlock, validate all expected versions, and only then
 * append — giving all-or-nothing semantics.</p>
 */
@Component
public class InMemoryEventStore implements EventStore {

    private final Map<String, List<StoredEvent>> streams = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final AtomicLong globalSequence = new AtomicLong(0);

    private ReentrantLock lockFor(String streamId) {
        return locks.computeIfAbsent(streamId, k -> new ReentrantLock());
    }

    @Override
    public List<StoredEvent> append(String streamId, long expectedVersion, List<AccountEvent> events) {
        ReentrantLock lock = lockFor(streamId);
        lock.lock();
        try {
            return appendLocked(streamId, expectedVersion, events);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<StoredEvent> appendAtomic(List<StreamAppend> appends) {
        // Lock every involved stream in a stable order (sorted by id) to prevent deadlock.
        List<StreamAppend> ordered = new ArrayList<>(appends);
        ordered.sort(Comparator.comparing(StreamAppend::streamId));

        Deque<ReentrantLock> acquired = new ArrayDeque<>();
        try {
            for (StreamAppend a : ordered) {
                ReentrantLock lock = lockFor(a.streamId());
                lock.lock();
                acquired.push(lock);
            }

            // Validate ALL expected versions before mutating ANY stream.
            for (StreamAppend a : ordered) {
                long current = currentVersion(a.streamId());
                if (current != a.expectedVersion()) {
                    throw new ConcurrencyException(a.streamId(), a.expectedVersion(), current);
                }
            }

            // All checks passed: append everything.
            List<StoredEvent> all = new ArrayList<>();
            for (StreamAppend a : ordered) {
                all.addAll(doAppend(a.streamId(), a.events()));
            }
            return all;
        } finally {
            while (!acquired.isEmpty()) {
                acquired.pop().unlock();
            }
        }
    }

    private List<StoredEvent> appendLocked(String streamId, long expectedVersion, List<AccountEvent> events) {
        long current = currentVersion(streamId);
        if (current != expectedVersion) {
            throw new ConcurrencyException(streamId, expectedVersion, current);
        }
        return doAppend(streamId, events);
    }

    /** Caller must already hold the stream lock. */
    private List<StoredEvent> doAppend(String streamId, List<AccountEvent> events) {
        List<StoredEvent> stream = streams.computeIfAbsent(streamId, k -> new CopyOnWriteArrayList<>());
        List<StoredEvent> appended = new ArrayList<>(events.size());
        long version = stream.size();
        for (AccountEvent event : events) {
            version++;
            StoredEvent stored = new StoredEvent(
                    globalSequence.incrementAndGet(),
                    streamId,
                    version,
                    event.getClass().getSimpleName(),
                    event,
                    Instant.now());
            stream.add(stored);
            appended.add(stored);
        }
        return appended;
    }

    private long currentVersion(String streamId) {
        List<StoredEvent> stream = streams.get(streamId);
        return stream == null ? 0L : stream.size();
    }

    @Override
    public List<StoredEvent> readStream(String streamId) {
        List<StoredEvent> stream = streams.get(streamId);
        return stream == null ? List.of() : List.copyOf(stream);
    }

    @Override
    public List<StoredEvent> readStreamUpToVersion(String streamId, long version) {
        return readStream(streamId).stream()
                .filter(e -> e.version() <= version)
                .toList();
    }

    @Override
    public List<StoredEvent> readStreamUpToTime(String streamId, Instant asOf) {
        return readStream(streamId).stream()
                .filter(e -> !e.recordedAt().isAfter(asOf))
                .toList();
    }

    @Override
    public List<StoredEvent> readAll() {
        return streams.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparingLong(StoredEvent::globalSequence))
                .toList();
    }

    @Override
    public boolean exists(String streamId) {
        List<StoredEvent> stream = streams.get(streamId);
        return stream != null && !stream.isEmpty();
    }
}
