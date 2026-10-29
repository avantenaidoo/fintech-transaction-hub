// Reconciliation Service Test
// Missing settlement CSV, Zero timeout and Negative timeout iwth IllegalStateException

package com.fintechhub.transactionapi.reconciliation;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fintechhub.transactionapi.transaction.TransactionRepository;

import tools.jackson.databind.json.JsonMapper;

class ReconciliationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void reconcileRejectsMissingSettlementFile() {
        var service = new ReconciliationService(
                mock(TransactionRepository.class),
                JsonMapper.builder().build(),
                "python",
                tempDir.toString(),
                "missing.csv",
                15);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                service::reconcile);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode());
    }

    @Test
    void reconcileRejectsZeroTimeout() {
        var service = new ReconciliationService(
                mock(TransactionRepository.class),
                JsonMapper.builder().build(),
                "python",
                tempDir.toString(),
                "missing.csv",
                0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::reconcile);

        assertEquals(
                "Reconciliation timeout must be greater than zero.",
                exception.getMessage());
    }

    @Test
    void reconcileRejectsNegativeTimeout() {
        var service = new ReconciliationService(
                mock(TransactionRepository.class),
                JsonMapper.builder().build(),
                "python",
                tempDir.toString(),
                "missing.csv",
                -1);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::reconcile);

        assertEquals(
                "Reconciliation timeout must be greater than zero.",
                exception.getMessage());
    }
}