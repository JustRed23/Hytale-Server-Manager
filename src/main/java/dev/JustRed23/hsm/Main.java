package dev.JustRed23.hsm;

public class Main {

    private final Arguments args;

    public Main(Arguments args) {
        this.args = args;
        System.out.println("Args: " + args);
    }

    static void main(String[] args) throws Exception {
        Arguments arguments = new Arguments(args);
        if (arguments.options.has(arguments.help)) {
            arguments.printHelp();
            return;
        }

        Main main = new Main(arguments);
    }
}
