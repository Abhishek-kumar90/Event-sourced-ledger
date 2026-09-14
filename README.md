# Event-Sourced Ledger

A small Spring Boot service that models bank-style accounts using **event sourcing**: the
source of truth is an **append-only log of events**, and current balances are always
*derived* by folding those events — never stored and mutated in place. It supports
deposits, withdrawals, atomic transfers, a fast read model, **time-travel** (rebuild any
account's state at any past point), **optimistic concurrency**, and **snapshots**.

# Event-Sourced Ledger

Spring Boot | Java 21 | Event Sourcing | JUnit

A Spring Boot REST API demonstrating:

• Event sourcing
• Immutable event log
• Event replay
• Time travel
• Projections
• Snapshots
• Optimistic concurrency
• Atomic transfers
• Exception handling
• Automated tests

```
Command → validate business rules → append immutable event(s)
                                          │
current state  ◄──── fold events ─────────┤
read model     ◄──── project events ──────┘
time-travel    ◄──── fold events up to a version / timestamp
```

## Why this project is interesting

It shows the core event-sourcing ideas that CRUD apps can't: an **immutable audit trail**
(every change is a recorded fact), **replay** (rebuild state, or any past state, from the
log), **projections** (disposable read models derived from events), and **consistency
under concurrency** (optimistic version checks, plus all-or-nothing multi-stream appends
for transfers). Validation happens at *command* time, so the log only ever contains
facts that really happened and replay can never fail.

## Tech stack

Java 21 · Spring Boot 3.3.5 · Spring Web · Bean Validation · JUnit 5. In-memory event
store, snapshot store, and read model.

## Quick start

```bash
mvn spring-boot:run
```

Open an account, deposit, and read it back:

```bash
# open with an initial balance
curl -s -XPOST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"owner":"Alice","initialDeposit":100.00}'
# -> {"accountId":"<id>","owner":"Alice","balance":100.00,"version":2}

# deposit
curl -s -XPOST localhost:8080/api/v1/accounts/<id>/deposit \
  -H 'Content-Type: application/json' -d '{"amount":50.00}'

# full audit trail
curl -s localhost:8080/api/v1/accounts/<id>/history

# time-travel: state as of version 2
curl -s "localhost:8080/api/v1/accounts/<id>?atVersion=2"
```

See **1_SETUP_AND_RUN.pdf** for the full API and examples, **2_PROJECT_EXPLAINED.pdf** to
understand and explain the whole design, **3_CODE_WALKTHROUGH.pdf** for a step-by-step
execution trace, and **4_BUGS_AND_FIXES.pdf** for the real bugs this project throws at you
while building it.

## Run the tests

```bash
mvn test
```

The tests exercise rebuild-from-events, overdraft rejection, atomic transfer, time-travel,
snapshotting, and optimistic-concurrency conflict — all in memory.

## Security notes

- All request input is validated at the boundary before any processing.
- Business rules (positive amounts, sufficient funds) are enforced at command time, so
  invalid operations never enter the log.
- Errors return safe messages only — no stack traces or internals leak.
- The event log is append-only: events are never updated or deleted.
