// PostgreSQL Integration test against fintech_hub_test database

package com.fintechhub.transactionapi.transaction;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionServiceIntegrationTest {

    private static final String KEY = "integration-payment-001";

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    // This is to cleanup existing test rows before each test
    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
    }

    @Test
    void shouldPersistTransactionInPostgreSql() {
        Transaction created = transactionService.createTransaction(
                "Integration payment",
                new BigDecimal("250.00"),
                CurrencyCode.ZAR,
                KEY
        );

        assertNotNull(created.getId());

        Transaction retrieved = transactionRepository
                .findById(created.getId())
                .orElseThrow();

        assertEquals("Integration payment", retrieved.getDescription());
        assertEquals(0, new BigDecimal("250.00")
                .compareTo(retrieved.getAmount()));
        assertEquals(CurrencyCode.ZAR, retrieved.getCurrency());
        assertEquals(KEY, retrieved.getIdempotencyKey());
        assertNotNull(retrieved.getCreatedAt());
        assertNotNull(retrieved.getUpdatedAt());
    }

    @Test
    void shouldNotCreateDuplicateForIdenticalRetry() {
        Transaction original = transactionService.createTransaction(
                "Integration payment",
                new BigDecimal("250.00"),
                CurrencyCode.ZAR,
                KEY
        );

        Transaction retry = transactionService.createTransaction(
                "Integration payment",
                new BigDecimal("250.00"),
                CurrencyCode.ZAR,
                KEY
        );

        assertEquals(original.getId(), retry.getId());
        assertEquals(1, transactionRepository.count());
    }

    @Test
    void shouldRejectSameKeyWithDifferentDetails() {
        transactionService.createTransaction(
                "Integration payment",
                new BigDecimal("250.00"),
                CurrencyCode.ZAR,
                KEY
        );

        assertThrows(
                IdempotencyConflictException.class,
                () -> transactionService.createTransaction(
                        "Different payment",
                        new BigDecimal("500.00"),
                        CurrencyCode.ZAR,
                        KEY
                )
        );

        assertEquals(1, transactionRepository.count());
    }
}