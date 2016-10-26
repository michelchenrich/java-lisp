package hm.lisp;

public class Interpreter {
    private Environment environment = new Environment();
    private Parser parser = new Parser(environment);

    public Interpreter() {
        loadStandardLibrary();
    }

    public void evaluate(String code) {
        parser.parse(code);
    }

    private void loadStandardLibrary() {
        evaluate("(define (+ x y) (<primitive>+ x y))");
        evaluate("(define (- x y) (<primitive>- x y))");
        evaluate("(define (* x y) (<primitive>* x y))");
        evaluate("(define (/ x y) (<primitive>/ x y))");
        evaluate("(define (= x y) (<primitive>= x y))");
        evaluate("(define (not x) (<primitive>not x))");
        evaluate("(define (> x y) (<primitive>> x y))");
        evaluate("(define (< x y) (<primitive>< x y))");
        evaluate("(define (>= x y) (<primitive>>= x y))");
        evaluate("(define (<= x y) (<primitive><= x y))");
        evaluate("(define (or x y) (<primitive>or x y))");
        evaluate("(define (and x y) (<primitive>and x y))");
        evaluate("(define (print x) (<primitive>print x))");
        evaluate("(define increment (+ 1))");
        evaluate("(define (decrement y) (<primitive>- y 1))");
        evaluate("(define pair (lambda (x y) (lambda (z) (if (= z 0) x y))))");
        evaluate("(define left (lambda (pair) (pair 0)))");
        evaluate("(define right (lambda (pair) (pair 1)))");
    }
}
