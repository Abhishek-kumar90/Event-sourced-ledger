package com.example.ledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request body to transfer money between two accounts.
 *
 * @param fromAccountId source account
 * @param toAccountId   destination account
 * @param amount        a strictly positive amount
 */
public record TransferRequest(
        @NotBlank(message = "fromAccountId is required")
        String fromAccountId,

        @NotBlank(message = "toAccountId is required")
        String toAccountId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount
) {
}
