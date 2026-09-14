package com.example.ledger.domain.event;

import java.math.BigDecimal;

/**
 * Recorded when money is removed from an account (a withdrawal, or the debit side of a
 * transfer).
 *
 * @param accountId the account debited
 * @param amount    a positive amount
 */
public record MoneyWithdrawn(String accountId, BigDecimal amount) implements AccountEvent {
}
