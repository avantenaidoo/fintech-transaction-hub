// Reconciliation Service Test
// Missing settlement CSV, Zero timeout and Negative timeout with IllegalStateException

package com.fintechhub.transactionapi.reconciliation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
                15,
                mock(ProcessLauncher.class));

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
                0,
                mock(ProcessLauncher.class));

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
                -1,
                mock(ProcessLauncher.class));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                service::reconcile);

        assertEquals(
                "Reconciliation timeout must be greater than zero.",
                exception.getMessage());
    }

    // Verify Python process failures return safe API responses:
    // HTTP 502 for process errors and invalid JSON; HTTP 504 for timeouts
    @Test
    void reconcileReturnsBadGatewayWhenPythonExitsWithError() throws Exception {
        createSettlementFile();

        Process process = mockProcess("", "script failed");
        when(process.waitFor(15, TimeUnit.SECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(1);

        var service = createService(mockLauncher(process));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                service::reconcile);

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals(
                "Python reconciliation failed.",
                exception.getReason());

        assertFalse(exception.getReason().contains("script failed"));
    }

    @Test
    void reconcileReturnsBadGatewayWhenPythonReturnsInvalidJson() throws Exception {
        createSettlementFile();

        Process process = mockProcess("this is not JSON", "");
        when(process.waitFor(15, TimeUnit.SECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(0);

        var service = createService(mockLauncher(process));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                service::reconcile);

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals(
                "Python returned invalid reconciliation JSON.",
                exception.getReason());
    }

    @Test
    void reconcileKillsPythonProcessWhenItTimesOut() throws Exception {
        createSettlementFile();

        Process process = mockProcess("", "");
        when(process.waitFor(15, TimeUnit.SECONDS)).thenReturn(false);
        when(process.isAlive()).thenReturn(false);

        var service = createService(mockLauncher(process));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                service::reconcile);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatusCode());
        assertEquals(
                "Python reconciliation exceeded its time limit.",
                exception.getReason());
        verify(process).destroyForcibly();
    }

    private Path createSettlementFile() throws Exception {
        Path file = tempDir.resolve("settlement.csv");
        Files.writeString(file, "reference,amount,currency\n");
        return file;
    }

    private Process mockProcess(String stdout, String stderr) throws Exception {
        Process process = mock(Process.class);

        when(process.getInputStream()).thenReturn(
                new ByteArrayInputStream(
                        stdout.getBytes(StandardCharsets.UTF_8)));

        when(process.getErrorStream()).thenReturn(
                new ByteArrayInputStream(
                        stderr.getBytes(StandardCharsets.UTF_8)));

        when(process.getOutputStream()).thenReturn(
                new ByteArrayOutputStream());

        return process;
    }

    private ProcessLauncher mockLauncher(Process process) throws Exception {
        ProcessLauncher launcher = mock(ProcessLauncher.class);

        when(launcher.start(anyList(), any(Path.class)))
                .thenReturn(process);

        return launcher;
    }

    private ReconciliationService createService(ProcessLauncher launcher) {
        return new ReconciliationService(
                mock(TransactionRepository.class),
                JsonMapper.builder().build(),
                "python",
                tempDir.toString(),
                "settlement.csv",
                15,
                launcher);
    }
}
