// This service compares amount numerically. Treats missing currency as ZAR

package com.fintechhub.transactionapi.transaction;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionWriter transactionWriter;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionWriter transactionWriter
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionWriter = transactionWriter;
    }

    @Transactional
    public Transaction createTransaction(
            String description,
            BigDecimal amount,
            CurrencyCode currency,
            String idempotencyKey
    ) {
        validateRequest(description, amount, idempotencyKey);

        CurrencyCode effectiveCurrency =
                currency == null ? CurrencyCode.ZAR : currency;

        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return verifyExistingRequest(
                    existing.get(), description, amount, effectiveCurrency
            );
        }

        Transaction transaction = new Transaction(
                description,
                amount,
                effectiveCurrency,
                idempotencyKey
        );

        try {
            return transactionWriter.save(transaction);
        } catch (DataIntegrityViolationException exception) {
            // Another request may have inserted this key concurrently.
            Transaction original = transactionRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);

            return verifyExistingRequest(
                    original, description, amount, effectiveCurrency
            );
        }
    }

    private Transaction verifyExistingRequest(
            Transaction original,
            String description,
            BigDecimal amount,
            CurrencyCode currency
    ) {
        boolean sameRequest =
                original.getDescription().equals(description)
                && original.getAmount().compareTo(amount) == 0
                && original.getCurrency() == currency;

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "This idempotency key has already been used "
                    + "with different transaction details."
            );
        }

        return original;
    }

    private void validateRequest(
            String description,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (description == null || description.isBlank()
                || description.length() > 255) {
            throw new IllegalArgumentException(
                    "Description must contain between 1 and 255 characters."
            );
        }

        if (amount == null || amount.signum() <= 0
                || amount.precision() - amount.scale() > 15
                || Math.max(amount.scale(), 0) > 4) {
            throw new IllegalArgumentException(
                    "Amount must be positive and support at most "
                    + "15 integer digits and 4 decimal places."
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency key must contain between 1 and 100 characters."
            );
        }
    }
}