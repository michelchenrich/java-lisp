package hm.lisp;

public class Interpreter {
    private Environment environment = new Environment();
    private final Parser parser = new Parser(environment);

    public void evaluate(String code) {
        parser.parse(code);
    }
}
