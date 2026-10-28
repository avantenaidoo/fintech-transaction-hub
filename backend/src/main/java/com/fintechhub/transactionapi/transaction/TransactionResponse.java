// Transaction response Data Transfer Object (DTO)

package com.fintechhub.transactionapi.transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String reference,
        String description,
        BigDecimal amount,
        CurrencyCode currency,
        TransactionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    // Including from Transaction transaction to keep the conversion in one place - prevent the REST controller from exposing it directly. Not exposing idempotency key yo the client
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}