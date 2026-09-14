package com.example.ledger.web;

import com.example.ledger.projection.BalanceView;
import com.example.ledger.projection.BalanceProjection;
import com.example.ledger.service.AccountService;
import com.example.ledger.web.dto.AccountResponse;
import com.example.ledger.web.dto.AmountRequest;
import com.example.ledger.web.dto.EventView;
import com.example.ledger.web.dto.OpenAccountRequest;
import com.example.ledger.web.dto.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * HTTP API for the ledger.
 *
 * <ul>
 *   <li>{@code POST /api/v1/accounts} — open an account</li>
 *   <li>{@code POST /api/v1/accounts/{id}/deposit} — deposit</li>
 *   <li>{@code POST /api/v1/accounts/{id}/withdraw} — withdraw</li>
 *   <li>{@code POST /api/v1/transfers} — transfer between accounts</li>
 *   <li>{@code GET  /api/v1/accounts/{id}} — current state, or historical via
 *       {@code ?atVersion=} / {@code ?asOf=}</li>
 *   <li>{@code GET  /api/v1/accounts/{id}/history} — the event audit trail</li>
 *   <li>{@code GET  /api/v1/accounts} — all balances from the read model</li>
 * </ul>
 *
 * <p>Note the two read paths: a single account is rebuilt from events (authoritative),
 * while the list of all accounts comes from the fast {@link BalanceProjection} read model
 * — a small, concrete demonstration of CQRS.</p>
 */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class LedgerController {

    private final AccountService accounts;
    private final BalanceProjection projection;

    public LedgerController(AccountService accounts, BalanceProjection projection) {
        this.accounts = accounts;
        this.projection = projection;
    }

    @PostMapping(value = "/accounts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest req) {
        AccountResponse body = AccountResponse.from(
                accounts.openAccount(req.owner(), req.initialDeposit()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping(value = "/accounts/{id}/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse deposit(@PathVariable String id, @Valid @RequestBody AmountRequest req) {
        return AccountResponse.from(accounts.deposit(id, req.amount()));
    }

    @PostMapping(value = "/accounts/{id}/withdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AccountResponse withdraw(@PathVariable String id, @Valid @RequestBody AmountRequest req) {
        return AccountResponse.from(accounts.withdraw(id, req.amount()));
    }

    @PostMapping(value = "/transfers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest req) {
        accounts.transfer(req.fromAccountId(), req.toAccountId(), req.amount());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse get(@PathVariable String id,
                              @RequestParam(required = false) Long atVersion,
                              @RequestParam(required = false) String asOf) {
        if (atVersion != null) {
            return AccountResponse.from(accounts.stateAtVersion(id, atVersion));
        }
        if (asOf != null) {
            Instant instant;
            try {
                instant = Instant.parse(asOf);
            } catch (java.time.format.DateTimeParseException e) {
                throw new com.example.ledger.exception.InvalidCommandException(
                        "asOf must be an ISO-8601 instant, e.g. 2026-08-02T09:00:00Z");
            }
            return AccountResponse.from(accounts.stateAsOf(id, instant));
        }
        return AccountResponse.from(accounts.load(id));
    }

    @GetMapping("/accounts/{id}/history")
    public List<EventView> history(@PathVariable String id) {
        return accounts.history(id).stream().map(EventView::from).toList();
    }

    @GetMapping("/accounts")
    public Collection<BalanceView> all() {
        return projection.getAll();
    }
}
