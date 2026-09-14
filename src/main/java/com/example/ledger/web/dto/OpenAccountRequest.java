package com.example.ledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Request body to open an account.
 *
 * @param owner          required owner name
 * @param initialDeposit optional opening balance (>= 0); null or 0 means open empty
 */
public record OpenAccountRequest(
        @NotBlank(message = "owner is required")
        String owner,

        @PositiveOrZero(message = "initialDeposit cannot be negative")
        BigDecimal initialDeposit
) {
}
