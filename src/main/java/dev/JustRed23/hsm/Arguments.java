package dev.JustRed23.hsm;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;

public final class Arguments {

    private final OptionParser parser = new OptionParser(false);
    public final OptionSet options;

    public final OptionSpec<Void> help;
    public final OptionSpec<Void> printServerHelp;
    public final OptionSpec<String> patchline;
    public final OptionSpec<Void> skipUpdate;

    Arguments(String[] args) {
        parser.allowsUnrecognizedOptions();

        help = parser.accepts("help", "Show this help message").forHelp();

        printServerHelp = parser.accepts("printServerHelp", "Prints the server help message and exits");

        patchline = parser.accepts("patchline")
                .withRequiredArg()
                .describedAs("The patchline to use")
                .defaultsTo("release");

        skipUpdate = parser.accepts("skipUpdate", "Skip the update check");

        options = parser.parse(args);
    }

    public <T> T get(OptionSpec<T> option) {
        return options.valueOf(option);
    }

    public boolean has(OptionSpec<?> option) {
        return options.has(option);
    }

    public void printHelp() throws IOException {
        parser.printHelpOn(System.out);
    }

    public String toString() {
        return "Arguments{" +
                "help=" + options.has(help) +
                ", patchline='" + options.valueOf(patchline) + '\'' +
                ", skipUpdate=" + options.has(skipUpdate) +
                ", extraArgs=" + options.nonOptionArguments() +
                '}';
    }
}
