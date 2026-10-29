// Test Python process failures in ReconcilliationService
// Python exits with an error, returns invalid JSON, takes too long and API should timeout

package com.fintechhub.transactionapi.reconciliation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface ProcessLauncher {

    Process start(List<String> command, Path workingDirectory)
            throws IOException;
}