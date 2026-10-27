// Using Junit and Mockito to test idempotency
// Tests mock TransactionWriter 
package com.fintechhub.transactionapi.transaction;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String KEY = "payment-abc123";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionWriter transactionWriter;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void shouldCreateNewTransaction() {
        when(transactionRepository.findByIdempotencyKey(KEY))
                .thenReturn(Optional.empty());

        when(transactionWriter.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.createTransaction(
                "Test payment",
                new BigDecimal("500.00"),
                CurrencyCode.ZAR,
                KEY
        );

        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals(CurrencyCode.ZAR, result.getCurrency());
        assertEquals(KEY, result.getIdempotencyKey());

        verify(transactionWriter).save(any(Transaction.class));
    }

    @Test
    void shouldReturnOriginalTransactionForIdenticalRetry() {
        Transaction original = new Transaction(
                "Test payment",
                new BigDecimal("500.00"),
                CurrencyCode.ZAR,
                KEY
        );

        when(transactionRepository.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(original));

        Transaction result = transactionService.createTransaction(
                "Test payment",
                new BigDecimal("500.0"),
                CurrencyCode.ZAR,
                KEY
        );

        assertSame(original, result);

        verify(transactionWriter, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectSameKeyWithDifferentAmount() {
        Transaction original = new Transaction(
                "Test payment",
                new BigDecimal("500.00"),
                CurrencyCode.ZAR,
                KEY
        );

        when(transactionRepository.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(original));

        IdempotencyConflictException exception = assertThrows(
                IdempotencyConflictException.class,
                () -> transactionService.createTransaction(
                        "Test payment",
                        new BigDecimal("900.00"),
                        CurrencyCode.ZAR,
                        KEY
                )
        );

        assertEquals(
                "This idempotency key has already been used "
                        + "with different transaction details.",
                exception.getMessage()
        );

        verify(transactionWriter, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectZeroAmount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        "Test payment",
                        BigDecimal.ZERO,
                        CurrencyCode.ZAR,
                        KEY
                )
        );

        assertEquals(
                "Amount must be positive and support at most "
                        + "15 integer digits and 4 decimal places.",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .findByIdempotencyKey(KEY);
        verify(transactionWriter, never()).save(any(Transaction.class));
    }
}