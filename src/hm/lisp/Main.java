package hm.lisp;

import java.io.BufferedInputStream;
import java.util.Scanner;

public class Main {
    public static void main(String[] arguments) throws InterruptedException {
        if (arguments.length == 0 || arguments.length > 2) {
            warnInvalidOptions();
        } else {
            switch (arguments[0]) {
                case "-h":
                    printHelp();
                    break;
                case "repl":
                    REPL();
                    break;
                case "run":
                    run(arguments[1]);
                    break;
                default:
                    warnInvalidOptions();
            }
        }
    }

    private static void run(String file) {

    }

    private static void REPL() throws InterruptedException {
        Interpreter interpreter = new Interpreter();
        System.out.println("Type (exit) to quit");
        Scanner scanner = new Scanner(new BufferedInputStream(System.in));
        while (true) {
            System.out.print("\n> ");
            String line = scanner.nextLine();
            if (line.equals("(exit)")) {
                System.out.println("Bye!");
                break;
            } else {
                System.out.print("=> ");
                interpreter.evaluate(line);
            }
        }

    }

    private static void printHelp() {
        System.out.println("repl : Start the REPL");
        System.out.println("run <path/to/file> : Runs the specified file");
    }

    private static void warnInvalidOptions() {
        System.out.println("Invalid usage, please run with option -h to see how to use it");
    }
}
