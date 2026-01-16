package dev.JustRed23.hsm;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;

public final class Arguments {

    private final OptionParser parser = new OptionParser(false);
    public final OptionSet options;

    public final OptionSpec<Void> help;
    public final OptionSpec<String> downloadPath;
    public final OptionSpec<String> patchline;
    public final OptionSpec<Void> skipUpdate;

    Arguments(String[] args) {
        parser.allowsUnrecognizedOptions();

        help = parser.accepts("help", "Show this help message").forHelp();

        downloadPath = parser.accepts("downloadPath")
                .withRequiredArg()
                .describedAs("The path to download the game zip file to")
                .defaultsTo("game.zip");

        patchline = parser.accepts("patchline")
                .withRequiredArg()
                .describedAs("The patchline to use")
                .defaultsTo("release");

        skipUpdate = parser.accepts("skipUpdate", "Skip the update check");

        options = parser.parse(args);
    }

    public void printHelp() throws IOException {
        parser.printHelpOn(System.out);
    }

    public String toString() {
        return "Arguments{" +
                "help=" + options.has(help) +
                ", downloadPath='" + options.valueOf(downloadPath) + '\'' +
                ", patchline='" + options.valueOf(patchline) + '\'' +
                ", skipUpdate=" + options.has(skipUpdate) +
                ", extraArgs=" + options.nonOptionArguments() +
                '}';
    }
}
