package hm.lisp;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class InterpreterTest {
    private ByteArrayOutputStream outputStream;
    private Interpreter interpreter;

    @Before
    public void setup() {
        this.outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(this.outputStream));
        interpreter = new Interpreter();
    }

    @Test
    public void variable() throws Exception {
        interpreter.evaluate("(define a 10)\n" +
                             "a");
        assertOutput("10.0");
    }

    @Test
    public void printString() throws Exception {
        interpreter.evaluate("(print \"hello world\")");
        assertOutput("hello world");
    }

    @Test
    public void printVariable() throws Exception {
        interpreter.evaluate("(define a \"hello world\")\n" +
                             "(print a)");
        assertOutput("hello world");
    }

    @Test
    public void printLambda() throws Exception {
        interpreter.evaluate("((lambda (x) (print x)) \"hello from lambda\")");
        assertOutput("hello from lambda");
    }

    @Test
    public void printSum() throws Exception {
        interpreter.evaluate("((lambda (x y) (print (+ x y))) 10 11)");
        assertOutput("21.0");
    }

    @Test
    public void printIdentity() throws Exception {
        interpreter.evaluate("(print ((lambda (x) x) \"identity\"))");
        assertOutput("identity");
    }

    @Test
    public void printFunction() throws Exception {
        interpreter.evaluate("(define add-5 (lambda (x) (+ x 5)))\n" +
                             "(print (add-5 10))");
        assertOutput("15.0");
    }

    @Test
    public void printHighOrderFunction() throws Exception {
        interpreter.evaluate("(define make-adder (lambda (x) (lambda (y) (+ x y))))\n" +
                             "(print ((make-adder 5) 10))");
        assertOutput("15.0");
    }

    private void assertOutput(String output) throws IOException {
        outputStream.flush();
        assertEquals(output, new String(outputStream.toByteArray()));
    }
}
