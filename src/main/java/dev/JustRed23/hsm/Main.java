package dev.JustRed23.hsm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.JustRed23.hsm.auth.Auth;
import dev.JustRed23.hsm.versioning.VersionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Arguments args;

    public Main(Arguments args) throws IOException {
        Main.args = args;
        LOGGER.debug("Starting with arguments: {}", args);

        if (!Auth.attemptLogin()) return;

        VersionManager manager = new VersionManager(args.has(args.skipUpdate), args.get(args.patchline));
        manager.run();
    }

    public static void startServer(String... launchArgs) {
        String[] extraArgs = args.has(args.printServerHelp) ? new String[] { "--help" } : args.getExtraArgs().toArray(new String[0]);
        try {
            List<String> allArgs = new ArrayList<>(List.of(launchArgs));
            allArgs.addAll(List.of(extraArgs));
            ProcessBuilder builder = new ProcessBuilder(allArgs);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            runIOThreads(process);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to start server process", e);
        }
    }

    private static void runIOThreads(Process process) throws InterruptedException {
        Thread outputThread = new Thread(() -> {
            Logger HYTALE_LOGGER = LogManager.getLogger("Hytale");
            try (var reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null && process.isAlive()) {
                    HYTALE_LOGGER.info(line);
                }
            } catch (IOException e) {
                LOGGER.error("Error reading server output", e);
            }
        });

        Thread inputThread = new Thread(() -> {
            try (
                    var writer = process.outputWriter();
                    var consoleReader = new BufferedReader(new InputStreamReader(System.in))
            ) {
                while (process.isAlive()) {
                    if (consoleReader.ready()) {
                        String line = consoleReader.readLine();
                        if (line == null) break;
                        if (line.isBlank()) continue;
                        writer.write(line);
                        writer.newLine();
                        writer.flush();
                    } else {
                        Thread.sleep(50);
                    }
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error writing to server input", e);
            }
        });

        outputThread.start();
        inputThread.start();

        int exitCode = process.waitFor();
        LOGGER.info("Server process exited with code: {}", exitCode);

        outputThread.join();
        inputThread.join();
    }

    static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments(args);
        if (arguments.options.has(arguments.help)) {
            arguments.printHelp();
            return;
        }

        new Main(arguments);
    }
}
