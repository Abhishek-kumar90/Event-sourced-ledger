package com.example.ledger.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body for a deposit or withdrawal.
 *
 * @param amount a strictly positive amount
 */
public record AmountRequest(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount
) {
}
