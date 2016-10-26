package hm.lisp;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class InterpreterTest {
    private PrintStream realOutput;
    private ByteArrayOutputStream outputStream;
    private Interpreter interpreter;

    @Before
    public void setup() {
        outputStream = new ByteArrayOutputStream();
        realOutput = System.out;
        System.setOut(new PrintStream(outputStream));
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

    @Test
    public void printInputFunction() throws Exception {
        interpreter.evaluate("(define print-f (lambda (f) (print (f))))\n" +
                             "(print-f (lambda () \"hello from input function\"))");
        assertOutput("hello from input function");
    }

    @Test
    public void printPair() throws Exception {
        interpreter.evaluate("(define nil \"null\")\n" +
                             "(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))\n" +
                             "(define left (lambda (list) (list 0)))\n" +
                             "(define right (lambda (list) (list 1)))\n" +
                             "(define print-pair (lambda (pair)\n" +
                             "  (do\n" +
                             "    (print (left pair))\n" +
                             "    (print \", \")\n" +
                             "    (print (right pair)))))\n" +
                             "(define one-two (pair 1 2))\n" +
                             "(print-pair one-two)");
        assertOutput("1.0, 2.0");
    }

    @Test
    public void printConditional() throws Exception {
        interpreter.evaluate("(define foo (lambda (x) (if (= x 0) (print \"true\") (print \"false\"))))\n" +
                             "(foo 0)\n" +
                             "(print \", \")\n" +
                             "(foo 1)");
        assertOutput("true, false");
    }

    @Test
    public void printCompleteList() throws Exception {
        interpreter.evaluate("(define nil \"null\")\n" +
                             "(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))\n" +
                             "(define left (lambda (list) (list 0)))\n" +
                             "(define right (lambda (list) (list 1)))\n" +
                             "(define print-list (lambda (list)\n" +
                             "  (if (= (right list) nil)\n" +
                             "    (print (left list))\n" +
                             "    (do\n" +
                             "      (print (left list))\n" +
                             "      (print \", \")\n" +
                             "      (print-list (right list))))))\n" +
                             "(define list (pair 1 (pair 2 (pair 3 (pair 4 nil)))))\n" +
                             "(print-list list)");
        assertOutput("1.0, 2.0, 3.0, 4.0");
    }

    @Test
    public void printNElementsFromList() throws Exception {
        interpreter.evaluate("(define nil \"null\")\n" +
                             "(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))\n" +
                             "(define left (lambda (list) (list 0)))\n" +
                             "(define right (lambda (list) (list 1)))\n" +
                             "(define print-list (lambda (list)\n" +
                             "  (if (= (right list) nil)\n" +
                             "    (print (left list))\n" +
                             "    (do\n" +
                             "      (print (left list))\n" +
                             "      (print \", \")\n" +
                             "      (print-list (right list))))))\n" +
                             "(define take (lambda (n list)\n" +
                             "  (if (= n 0)\n" +
                             "    nil\n" +
                             "    (pair\n" +
                             "      (left list)\n" +
                             "      (take (decrement n) (right list))))))\n" +
                             "(define list (pair 1 (pair 2 (pair 3 (pair 4 nil)))))\n" +
                             "(print-list (take 3 list))");
        assertOutput("1.0, 2.0, 3.0");
    }

    @Test
    public void conflictingNames() throws Exception {
        interpreter.evaluate("(define x 10)\n" +
                             "(define delayed-adder (lambda (x)\n" +
                             "  (+ ((lambda (x) x) 5) x)))\n" +
                             "(print (delayed-adder x))");
        assertOutput("15.0");
    }

    @Test
    public void sameVariableNames() throws Exception {
        interpreter.evaluate("(define identity (lambda (x) x))\n" +
                             "(define apply (lambda (x y) (x y)))\n" +
                             "(define delayed-adder (lambda (x)\n" +
                             "  (+ (apply identity (apply identity (apply identity 5)))\n" +
                             "     (apply (identity identity) (identity x)))))\n" +
                             "(print (delayed-adder 10))");
        assertOutput("15.0");
    }

    @Test
    public void reverseListBug() throws Exception {
        interpreter.evaluate("(define nil \"null\")\n" +
                             "(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))\n" +
                             "(define left (lambda (list) (list 0)))\n" +
                             "(define right (lambda (list) (list 1)))\n" +
                             "(define list (pair 3 (pair 2 (pair 1 nil))))" +
                             "(print (left (right (right list))))");
        assertOutput("1.0");
    }

    @Test
    public void printReverseList() throws Exception {
        interpreter.evaluate("(define nil \"null\")\n" +
                             "(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))\n" +
                             "(define left (lambda (list) (list 0)))\n" +
                             "(define right (lambda (list) (list 1)))\n" +
                             "(define print-list (lambda (list)\n" +
                             "  (if (= (right list) nil)\n" +
                             "    (print (left list))\n" +
                             "    (do\n" +
                             "      (print (left list))\n" +
                             "      (print \", \")\n" +
                             "      (print-list (right list))))))\n" +
                             "(define countdown (lambda (n)\n" +
                             "  (if (= n 0)\n" +
                             "    nil\n" +
                             "    (pair n (countdown (decrement n))))))\n" +
                             "(define list (countdown 3))\n" +
                             "(print-list list)");
        assertOutput("3.0, 2.0, 1.0");
    }

    @Test
    public void printLet() throws Exception {
        interpreter.evaluate("(define greet (lambda (name sex)\n" +
                             "  (let ((ms \"Miss\")\n" +
                             "        (mr \"Mister\"))\n" +
                             "    (do\n" +
                             "      (print (if (= sex \"male\")\n" +
                             "               mr\n" +
                             "               ms))\n" +
                             "      (print \" \")\n" +
                             "      (print name)))))\n" +
                             "(greet \"Bruna\" \"female\")");
        assertOutput("Miss Bruna");
    }

    @Test
    public void printUpdatingLet() throws Exception {
        interpreter.evaluate("(print (let ((x 10)\n" +
                             "             (x (increment x)))\n" +
                             "         x))");
        assertOutput("11.0");
    }

    @Test
    public void printLetInLambda() throws Exception {
        interpreter.evaluate("(define print-increment (lambda (x)\n" +
                             "  (let ((x (increment x)))\n" +
                             "    (do (print 10.0) (print \", \") (print x)))))\n" +
                             "(print-increment 10.0)");
        assertOutput("10.0, 11.0");
    }

    @Test
    public void functionDefinitionShortcut() throws Exception {
        interpreter.evaluate("(define (print-increment x)\n" +
                             "  (let ((x (increment x)))\n" +
                             "    (do (print 10.0) (print \", \") (print x))))\n" +
                             "(print-increment 10.0)");
        assertOutput("10.0, 11.0");
    }

    @Test
    public void recurse() throws Exception {
        interpreter.evaluate("(define (factorial n)" +
                             "  (if (= n 0)" +
                             "    1" +
                             "    (* n (factorial (decrement n)))))" +
                             "(print (factorial 5))");
        assertOutput("120.0");
    }

    @Test
    public void delayLambdaExpansion() throws Exception {
        interpreter.evaluate("(define (partial f x) (lambda (y) (f x y)))\n" +
                             "(define (power n x) (if (= n 1) x (* x (power (decrement n) x))))\n" +
                             "(define square (partial power 2))\n" +
                             "square");
        assertOutput("(lambda (y) ((lambda (n x) (if (= n 1.0) x (* x (power (decrement n) x)))) 2.0 y))");
    }

    @Test
    public void primitiveAutoCurry() throws Exception {
        interpreter.evaluate("(print ((+ 5) 10))");
        assertOutput("15.0");
    }

    @Ignore
    @Test
    public void primitiveAutoCurryDescription() throws Exception {
        interpreter.evaluate("(print (+ 5))");
        assertOutput("(lambda (y) (+ 5 y))");
    }

    @Test
    public void autoCurry() throws Exception {
        interpreter.evaluate("(define (power n x) (if (= n 1) x (* x (power (decrement n) x))))\n" +
                             "(print ((power 2) 5))");
        assertOutput("25.0");
    }

    @Ignore
    @Test
    public void autoCurryDescription() throws Exception {
        interpreter.evaluate("(define (power n x) (if (= n 1) x (* x (power (decrement n) x))))\n" +
                             "(print (power 2))");
        assertOutput("(lambda (x) (power 2.0 x))");
    }

    @Test
    public void lambdaAutoCurry() throws Exception {
        interpreter.evaluate("(print (((lambda (x y) (* x y)) 5) 10))");
        assertOutput("50.0");
    }

    @Test
    public void lambdaAutoCurryDescription() throws Exception {
        interpreter.evaluate("(print ((lambda (n x) (if (= n 1) x (* x (power (decrement n) x)))) 2))");
        assertOutput("(lambda (x) (if (= 2.0 1.0) x (* x (power (decrement 2.0) x))))");
    }

    @Test
    public void internalFunctions() throws Exception {
        interpreter.evaluate("(define (countup x)" +
                             "  (let ((iteration (lambda (y x)" +
                             "                     (if (= y x) " +
                             "                       (print y)" +
                             "                       (do (print y)" +
                             "                           (print \", \")" +
                             "                           (iteration (increment y) x))))))" +
                             "    (iteration 1 x)))" +
                             "(countup 10)");
        assertOutput("1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0");
    }

    private void assertOutput(String output) throws IOException {
        outputStream.flush();
        assertEquals(output, new String(outputStream.toByteArray()));
    }
}
