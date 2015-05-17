import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.*;
import java.util.ArrayDeque;

public class JSDiffTest {
    public static class IO {
        public String stringResult;
        public double[][][] table = new double[10][10][10];

        public void print(String message) {
            System.out.print(message);
        }
        public void println(String message) {
            System.out.println(message);
        }
    }

    private final ScriptEngineManager factory = new ScriptEngineManager();
    public static void main(String[] args) throws ScriptException, IOException {
        checkAssert();

        System.out.println("Options:\n    -easy    -- easy version\n    -verbose -- verbose output");
        boolean verbose = false;
        boolean easy = false;
        for (String arg : args) {
            switch (arg) {
                case "-verbose":
                    verbose = true;
                    break;
                case "-easy":
                    easy = true;
                    break;
                default:
                    throw new AssertionError("Unknown option " + arg);
            }
        }
        new JSDiffTest(verbose, easy).run();
    }

    private boolean verbose;
    private boolean easy;

    public JSDiffTest(boolean verbose, boolean easy) {
        this.verbose = verbose;
        this.easy = easy;
    }

    private void run() {
        test("x");
        test("y");
        test("z");
        test("10");
        test("x y +");
        test("x y -");
        test("x y *");
        test("x 5 /");
        test("x sin");
        test("x cos");
        test("2 x * 3 -");
        test("x x 2 - * x * 1 +");

        int base = test;
        while (test < TOTAL_TESTS) {
            test(polish(randomExpression((test - base + 1) * 3)));
        }
    }

    private final Random random = new Random(8924702402404781470L);
    private DiffExpression randomExpression(int size) {
        if (size <= 0) {
            if (random.nextBoolean()) {
                return new Const(random.nextInt(1000));
            } else {
                return VARIABLES[random.nextInt(VARIABLES.length)];
            }
        }
        switch (random.nextInt(6)) {
            case 0: return new Add(randomExpression(size / 2 - 1), randomExpression(size / 2));
            case 1: return new Subtract(randomExpression(size / 2 - 1), randomExpression(size / 2));
            case 2: return new Multiply(randomExpression(size / 2 - 1), randomExpression(size / 2));
            case 3: return new Divide(randomExpression(size / 2 - 1), randomExpression(size / 2));
            case 4: return new Sin(randomExpression(size - 1));
            case 5: return new Cos(randomExpression(size - 1));
            default: throw new AssertionError("");
        }
    }

    private int test = 0;
    private int TOTAL_TESTS = 30;
    private void test(String s) {
        test++;
        if (easy) {
            s = s.replaceAll("y|z", "x");
        }
        System.out.format("Test %d of %d: %s\n", test, TOTAL_TESTS, s);
        final DiffExpression expression = parse(s);
        testToPolish(expression);
        if (!easy) {
            testParseTabulate(expression);
        }
        testTabulate(expression);
        testDiff(expression);
        testDiff2(expression);
    }

    private void testTabulate(DiffExpression expression) {
        part("    Testing evaluate(...)");
        String script =
                "var expr = " + expression(expression) + ";" +
                        "for (var x = 0; x < 10; x++) for (var y = 0; y < 10; y++) for (var z = 0; z < 10; z++)" +
                        "io.table[x][y][z] = expr.evaluate(x, y, z);";
        checkEquals(expression, run(script).table);
    }

    private void testParseTabulate(DiffExpression expression) {
        part("    Testing parse(expr).evaluate(...)");
        String script =
                "var expr = parse('" + polish(expression) + "');" +
                        "for (var x = 0; x < 10; x++) for (var y = 0; y < 10; y++) for (var z = 0; z < 10; z++)" +
                        "io.table[x][y][z] = expr.evaluate(x, y, z);";
        checkEquals(expression, run(script).table);
    }

    private void part(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    private static void checkEquals(DiffExpression expected, double[][][] actual) {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                for (int z = 0; z < 10; z++) {
                    double e = expected.evaluate(x, y, z);
                    double a = actual[x][y][z];
                    if (!isEqual(e, a)) {
                        throw new AssertionError(String.format("x = %d, y = %d, z = %d: expected %f, actual %f", x, y, z, e, a));
                    }
                }
            }
        }
    }

    private void testToPolish(DiffExpression expression) {
        part("    Testing toString()");
        String script = "io.stringResult = (" + expression(expression) + ").toString()";
        checkEquals(expression, parse(run(script).stringResult));
    }

    private void testDiff(DiffExpression expression) {
        for (String var : VARS) {
            DiffExpression diff = expression.diff(var);
            part("    Testing diff('" + var + "').toString()");
            String toStringScript = "io.stringResult = (" + expression(expression) + ").diff('" + var + "').toString()";
            checkEquals(diff, parse(run(toStringScript).stringResult));

            part("    Testing diff('" + var + "').evaluate(...)");
            String script =
                    "var expr = " + expression(expression) + ".diff('" + var + "');" +
                            "for (var x = 0; x < 10; x++) for (var y = 0; y < 10; y++) for (var z = 0; z < 10; z++)" +
                            "io.table[x][y][z] = expr.evaluate(x, y, z);";
            checkEquals(diff, run(script).table);
            if (easy) {
                break;
            }
        }
    }

    private void testDiff2(DiffExpression expression) {
        for (String var1 : VARS) {
            for (String var2 : VARS) {
                DiffExpression diff = expression.diff(var1).diff(var2);
                part("    Testing diff('" + var1 + "').diff('" + var2 + "').toString()");
                String toStringScript = "io.stringResult = (" + expression(expression) + ").diff('" + var1 + "').diff('" + var2 + "').toString()";
                checkEquals(diff, parse(run(toStringScript).stringResult));

                if (easy) {
                    break;
                }
            }
            if (easy) {
                break;
            }
        }
    }

    private IO run(String script) {
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        final IO io = new IO();
        engine.put("io", io);
        try {
            engine.eval(
                    "println = function(message) { io.println(message); };" +
                            "print   = function(message) { io.print  (message); };"
            );
            engine.eval(new InputStreamReader(new FileInputStream("jsdiff.js"), "UTF-8"));
            engine.eval(script);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return io;
    }

    private static void checkEquals(DiffExpression expected, DiffExpression actual) {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                for (int z = 0; z < 10; z++) {
                    double e = expected.evaluate(x, y, z);
                    double a = actual.evaluate(x, y, z);
                    if (!isEqual(e, a)) {
                        throw new AssertionError(String.format("x = %d, y = %d, z = %d: expected %f, actual %f", x, y, z, e, a));
                    }
                }
            }
        }
    }

    private static final double EPS = 1e-5;

    private static boolean isEqual(double expected, double actual) {
        return Double.isNaN(expected) && Double.isNaN(actual)
                || expected == actual
                || Math.abs(expected - actual) < EPS
                || Math.abs(expected) >= 1 && Math.abs(expected - actual) / expected < EPS;
    }

    private static void checkAssert() {
        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            throw new AssertionError("You should enable assertions by running 'java -ea JSDiffTest'");
        }
    }

    private static String polish(DiffExpression expression) {
        return expression.polish(new StringBuilder()).toString().trim();
    }

    private static String expression(DiffExpression expression) {
        return expression.expression(new StringBuilder()).toString().trim();
    }

    private final static Variable X = new Variable("x");
    private final static Variable Y = new Variable("y");
    private final static Variable Z = new Variable("z");
    private final static String[] VARS = {"x", "y", "z"};
    private final static Variable[] VARIABLES = {X, Y, Z};

    private static DiffExpression parse(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s);
        final Deque<DiffExpression> stack = new ArrayDeque<>();
        try {
            while (tokenizer.hasMoreTokens()) {
                final String token = tokenizer.nextToken();
                if ('0' <= token.charAt(0) && token.charAt(0) <= '9') {
                    try {
                        stack.addLast(new Const(Double.valueOf(token)));
                    } catch (NumberFormatException e) {
                        throw new AssertionError("Invalid number " + token);
                    }
                } else {
                    DiffExpression p;
                    switch (token) {
                        case "x":
                            stack.addLast(X);
                            break;
                        case "y":
                            stack.addLast(Y);
                            break;
                        case "z":
                            stack.addLast(Z);
                            break;
                        case "+":
                            p = stack.removeLast();
                            stack.addLast(new Add(stack.removeLast(), p));
                            break;
                        case "-":
                            p = stack.removeLast();
                            stack.addLast(new Subtract(stack.removeLast(), p));
                            break;
                        case "*":
                            p = stack.removeLast();
                            stack.addLast(new Multiply(stack.removeLast(), p));
                            break;
                        case "/":
                            p = stack.removeLast();
                            stack.addLast(new Divide(stack.removeLast(), p));
                            break;
                        case "sin":
                            stack.addLast(new Sin(stack.removeLast()));
                            break;
                        case "cos":
                            stack.addLast(new Cos(stack.removeLast()));
                            break;
                        default:
                            throw new AssertionError("Invalid token " + token);
                    }
                }
            }
        } catch (NoSuchElementException e) {
            throw new AssertionError("Invalid expression " + s);
        }
        assert stack.size() == 1 : "Invalid expression " + s;
        return stack.getFirst();
    }

    interface DiffExpression {
        String name();
        String symbol();
        double evaluate(double x, double y, double z);
        DiffExpression diff(String variable);

        StringBuilder polish(StringBuilder sb);
        StringBuilder expression(StringBuilder sb);
    }

    static final Const ZERO = new Const(0);
    static final Const ONE = new Const(1);
    static abstract class NullaryOperation implements DiffExpression {
        @Override
        public StringBuilder polish(StringBuilder sb) {
            return sb.append(symbol()).append(" ");
        }

        @Override
        public StringBuilder expression(StringBuilder sb) {
            return sb.append(name());
        }
    }

    static class Const extends NullaryOperation {
        private final double value;

        Const(double value) {
            this.value = value;
        }

        @Override
        public double evaluate(double x, double y, double z) {
            return value;
        }

        @Override
        public DiffExpression diff(String variable) {
            return ZERO;
        }

        @Override
        public String name() {
            return "new Const(" + value + ")";
        }

        @Override
        public String symbol() {
            return String.valueOf(value);
        }
    }

    static class Variable extends NullaryOperation {
        private final String name;

        Variable(String name) {
            this.name = name;
            evaluate(0, 0, 0);
        }

        @Override
        public double evaluate(double x, double y, double z) {
            switch (name) {
                case "x": return x;
                case "y": return y;
                case "z": return z;
                default: throw new AssertionError("Unknown variable " + name);
            }
        }

        @Override
        public DiffExpression diff(String variable) {
            return name.equals(variable) ? ONE : ZERO;
        }

        @Override
        public String name() {
            return "new Variable('" + name + "')";
        }

        @Override
        public String symbol() {
            return name;
        }
    }

    static abstract class BinaryOperation implements DiffExpression {
        private final DiffExpression op1;
        private final DiffExpression op2;

        protected BinaryOperation(DiffExpression op1, DiffExpression op2) {
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public double evaluate(double x, double y, double z) {
            return evaluate(op1.evaluate(x, y, z), op2.evaluate(x, y, z));
        }

        protected abstract double evaluate(double a, double b);

        @Override
        public DiffExpression diff(String variable) {
            return diff(op1, op1.diff(variable), op2, op2.diff(variable));
        }

        protected abstract DiffExpression diff(DiffExpression a, DiffExpression da, DiffExpression b, DiffExpression db);

        @Override
        public final String name() {
            return getClass().getSimpleName();
        }

        @Override
        public StringBuilder polish(StringBuilder sb) {
            return op2.polish(op1.polish(sb)).append(symbol()).append(" ");
        }

        @Override
        public StringBuilder expression(StringBuilder sb) {
            return op2.expression(op1.expression(sb.append("new ").append(name()).append("(")).append(", ")).append(")");
        }
    }

    static class Add extends BinaryOperation {
        public Add(DiffExpression op1, DiffExpression op2) {
            super(op1, op2);
        }

        @Override
        protected double evaluate(double a, double b) {
            return a + b;
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da, DiffExpression b, DiffExpression db) {
            return new Add(da, db);
        }

        @Override
        public String symbol() {
            return "+";
        }
    }

    static class Subtract extends BinaryOperation {
        Subtract(DiffExpression op1, DiffExpression op2) {
            super(op1, op2);
        }

        @Override
        protected double evaluate(double a, double b) {
            return a - b;
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da, DiffExpression b, DiffExpression db) {
            return new Subtract(da, db);
        }

        @Override
        public String symbol() {
            return "-";
        }
    }

    static class Multiply extends BinaryOperation {
        Multiply(DiffExpression op1, DiffExpression op2) {
            super(op1, op2);
        }

        @Override
        protected double evaluate(double a, double b) {
            return a * b;
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da, DiffExpression b, DiffExpression db) {
            return new Add(new Multiply(a, db), new Multiply(b, da));
        }

       @Override
        public String symbol() {
            return "*";
        }
    }

    static class Divide extends BinaryOperation {
        Divide(DiffExpression op1, DiffExpression op2) {
            super(op1, op2);
        }

        @Override
        protected double evaluate(double a, double b) {
            return a / b;
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da, DiffExpression b, DiffExpression db) {
            return new Divide(
                    new Subtract(new Multiply(da, b), new Multiply(a, db)),
                    new Multiply(b, b)
            );
        }

        @Override
        public String symbol() {
            return "/";
        }
    }

    static abstract class UnaryOperation implements DiffExpression {
        private final DiffExpression op;

        protected UnaryOperation(DiffExpression op) {
            this.op = op;
        }

        @Override
        public double evaluate(double x, double y, double z) {
            return evaluate(op.evaluate(x, y, z));
        }

        protected abstract double evaluate(double a);

        @Override
        public DiffExpression diff(String variable) {
            return diff(op, op.diff(variable));
        }

        protected abstract DiffExpression diff(DiffExpression a, DiffExpression da);

        @Override
        public String name() {
            return getClass().getSimpleName();
        }

        @Override
        public String symbol() {
            return name().toLowerCase();
        }


        @Override
        public StringBuilder polish(StringBuilder sb) {
            return op.polish(sb).append(symbol()).append(" ");
        }

        @Override
        public StringBuilder expression(StringBuilder sb) {
            return op.expression(sb.append("new ").append(name()).append("(")).append(")");
        }
    }

    static class Sin extends UnaryOperation {
        Sin(DiffExpression op) {
            super(op);
        }

        @Override
        protected double evaluate(double a) {
            return Math.sin(a);
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da) {
            return new Multiply(new Cos(a), da);
        }
    }

    static class Cos extends UnaryOperation {
        Cos(DiffExpression op) {
            super(op);
        }

        @Override
        protected double evaluate(double a) {
            return Math.cos(a);
        }

        @Override
        protected DiffExpression diff(DiffExpression a, DiffExpression da) {
            return new Subtract(ZERO, new Multiply(new Sin(a), da));
        }
    }
}