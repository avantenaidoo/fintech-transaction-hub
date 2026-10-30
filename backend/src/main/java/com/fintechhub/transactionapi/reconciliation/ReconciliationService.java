// Reconciliation through Spring Boot
// Retrieves saved transactions, Sends refs to Python, Run reconciliation.src.cli without a shell, Returns Python JSON results and reports HTTP errors

package com.fintechhub.transactionapi.reconciliation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fintechhub.transactionapi.transaction.Transaction;
import com.fintechhub.transactionapi.transaction.TransactionRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ReconciliationService {

	private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

	private final TransactionRepository transactionRepository;
	private final JsonMapper objectMapper;
	private final String pythonCommand;
	private final Path projectRoot;
	private final Path settlementFile;
	private final long timeoutSeconds;
	private final ProcessLauncher processLauncher;

	public ReconciliationService(
			TransactionRepository transactionRepository,
			JsonMapper objectMapper,
			@Value("${reconciliation.python-command}") String pythonCommand,
			@Value("${reconciliation.project-root}") String projectRoot,
			@Value("${reconciliation.settlement-file}") String settlementFile,
			@Value("${reconciliation.timeout-seconds:15}") long timeoutSeconds,
			ProcessLauncher processLauncher) {

		this.transactionRepository = transactionRepository;
		this.objectMapper = objectMapper;
		this.pythonCommand = pythonCommand;
		this.projectRoot = Path.of(projectRoot)
				.toAbsolutePath()
				.normalize();
		this.settlementFile = this.projectRoot
				.resolve(settlementFile)
				.normalize();
		this.timeoutSeconds = timeoutSeconds;
		this.processLauncher = processLauncher;
	}

	public JsonNode reconcile() {
		if (timeoutSeconds <= 0) {
			throw new IllegalStateException(
					"Reconciliation timeout must be greater than zero.");
		}

		if (!settlementFile.startsWith(projectRoot)
				|| !java.nio.file.Files.isRegularFile(settlementFile)) {
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"The configured settlement CSV file is unavailable.");
		}

		List<Map<String, String>> internalTransactions = transactionRepository.findAll()
				.stream()
				.map(this::toReconciliationInput)
				.toList();

		Process process = null;

		try {
			process = processLauncher.start(
					List.of(
							pythonCommand,
							"-m",
							"reconciliation.src.cli",
							"--settlement-file",
							settlementFile.toString()),
					projectRoot);

			Process runningProcess = process;

			CompletableFuture<byte[]> stdout = CompletableFuture.supplyAsync(
					() -> readOutput(runningProcess.getInputStream()));
			CompletableFuture<byte[]> stderr = CompletableFuture.supplyAsync(
					() -> readOutput(runningProcess.getErrorStream()));

			try (var stdin = process.getOutputStream()) {
				objectMapper.writeValue(stdin, internalTransactions);
			}

			if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
				logger.warn(
						"Python reconciliation exceeded the {} second time limit.",
						timeoutSeconds);

				process.destroyForcibly();

				throw new ResponseStatusException(
						HttpStatus.GATEWAY_TIMEOUT,
						"Python reconciliation exceeded its time limit.");
			}

			String output = new String(
					stdout.get(2, TimeUnit.SECONDS),
					StandardCharsets.UTF_8);
			String errorOutput = new String(
					stderr.get(2, TimeUnit.SECONDS),
					StandardCharsets.UTF_8);

			if (process.exitValue() != 0) {
				logger.error(
						"Python reconciliation failed with exit code {}: {}",
						process.exitValue(),
						errorOutput.trim());

				throw new ResponseStatusException(
						HttpStatus.BAD_GATEWAY,
						"Python reconciliation failed.");
			}

			try {
				return objectMapper.readTree(output);
			} catch (JacksonException exception) {
				logger.error(
						"Python returned invalid reconciliation JSON.",
						exception);

				throw new ResponseStatusException(
						HttpStatus.BAD_GATEWAY,
						"Python returned invalid reconciliation JSON.");
			}

		} catch (IOException exception) {
			logger.error(
					"Unable to start or communicate with Python.",
					exception);

			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Unable to start or communicate with Python.");

		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();

			logger.error(
					"Reconciliation was interrupted.",
					exception);

			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Reconciliation was interrupted.");

		} catch (ExecutionException exception) {
			logger.error(
					"Unable to read Python reconciliation output.",
					exception);

			throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Unable to read Python reconciliation output.");

		} catch (java.util.concurrent.TimeoutException exception) {
			logger.error(
					"Timed out reading Python reconciliation output.",
					exception);

			throw new ResponseStatusException(
					HttpStatus.GATEWAY_TIMEOUT,
					"Timed out reading Python reconciliation output.");
		} finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	private Map<String, String> toReconciliationInput(
			Transaction transaction) {
		return Map.of(
				"reference", transaction.getReference(),
				"amount", transaction.getAmount().toPlainString(),
				"currency", transaction.getCurrency().name());
	}

	private byte[] readOutput(InputStream inputStream) {
		try (inputStream) {
			return inputStream.readAllBytes();
		} catch (IOException exception) {
			throw new IllegalStateException(
					"Failed to read Python process output.",
					exception);
		}
	}
}