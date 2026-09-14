package com.example.ledger.domain.event;

import java.math.BigDecimal;

/**
 * Recorded when money is added to an account (a deposit, or the credit side of a
 * transfer).
 *
 * @param accountId the account credited
 * @param amount    a positive amount
 */
public record MoneyDeposited(String accountId, BigDecimal amount) implements AccountEvent {
}
