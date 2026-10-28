// Transaction request Data Transfer Object(DTO)

package com.fintechhub.transactionapi.transaction;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(

        @NotBlank(message = "Description is required.")
        @Size(max = 255, message = "Description must not exceed 255 characters.")
        String description,

        @DecimalMin(value = "0.0001", message = "Amount must be greater than zero.")
        @DecimalMax(value = "999999999999999.9999",
                message = "Amount exceeds the supported maximum.")
        @Digits(integer = 15, fraction = 4,
                message = "Amount must support at most 15 integer digits and 4 decimal places.")
        BigDecimal amount,

        CurrencyCode currency,

        @NotBlank(message = "Idempotency key is required.")
        @Size(max = 100, message = "Idempotency key must not exceed 100 characters.")
        String idempotencyKey
) {
}