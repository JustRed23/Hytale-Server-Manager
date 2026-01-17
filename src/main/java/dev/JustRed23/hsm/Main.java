package dev.JustRed23.hsm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.JustRed23.hsm.auth.Auth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    public static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Arguments args;

    public Main(Arguments args) {
        Main.args = args;
        LOGGER.debug("Starting with arguments: {}", args);

        if (!Auth.attemptLogin()) return;

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
