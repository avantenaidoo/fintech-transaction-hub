// Focused unit tests

package com.fintechhub.transactionapi.transaction;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void shouldApplyDefaultCurrencyAndStatus() {
        Transaction transaction = new Transaction(
                "Test payment",
                new BigDecimal("125.50"),
                null,
                "idem-test-001"
        );

        assertEquals(CurrencyCode.ZAR, transaction.getCurrency());
        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        assertNotNull(transaction.getReference());
        assertTrue(transaction.getReference().startsWith("txn_"));
    }

    @Test
    void shouldPreserveAmountPrecisely() {
        BigDecimal amount = new BigDecimal("125.1234");

        Transaction transaction = new Transaction(
                "Test payment",
                amount,
                CurrencyCode.USD,
                "idem-test-002"
        );

        assertEquals(amount, transaction.getAmount());
        assertEquals(CurrencyCode.USD, transaction.getCurrency());
    }

    @Test
    void shouldRejectZeroOrNegativeAmounts() {
        Transaction zeroAmount = new Transaction(
                "Zero payment",
                BigDecimal.ZERO,
                CurrencyCode.ZAR,
                "idem-test-003"
        );

        Transaction negativeAmount = new Transaction(
                "Negative payment",
                new BigDecimal("-10.00"),
                CurrencyCode.ZAR,
                "idem-test-004"
        );

        assertFalse(validator.validate(zeroAmount).isEmpty());
        assertFalse(validator.validate(negativeAmount).isEmpty());
    }

    @Test
    void shouldRejectBlankDescriptionsAndIdempotencyKeys() {
        Transaction transaction = new Transaction(
                " ",
                new BigDecimal("10.00"),
                CurrencyCode.ZAR,
                " "
        );

        var violations = validator.validate(transaction);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString()
                        .equals("description")
        ));
        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString()
                        .equals("idempotencyKey")
        ));
    }
}