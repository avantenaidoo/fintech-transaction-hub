// Keep real process-launching behaviour while allowing tests

package com.fintechhub.transactionapi.reconciliation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SystemProcessLauncher implements ProcessLauncher {

    @Override
    public Process start(List<String> command, Path workingDirectory)
            throws IOException {

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());

        return builder.start();
    }
}