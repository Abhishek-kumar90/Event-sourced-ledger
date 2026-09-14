package com.example.ledger.web.dto;

import com.example.ledger.domain.model.Account;

import java.math.BigDecimal;

/**
 * The account state returned to clients.
 *
 * @param accountId id
 * @param owner     owner name
 * @param balance   current (or historical) balance
 * @param version   the event version this state reflects
 */
public record AccountResponse(String accountId, String owner, BigDecimal balance, long version) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.id(), account.owner(), account.balance(), account.version());
    }
}
