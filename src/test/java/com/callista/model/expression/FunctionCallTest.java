package com.callista.model.expression;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionCallTest {

    private static final double DELTA = 1e-9;

    private final Variable x = new Variable("x");

    private static Constant c(double value) {
        return new Constant(value);
    }

    private FunctionCall call(MathFunction function, Expression argument) {
        return new FunctionCall(function, argument);
    }

    @Test
    void evaluatesTrigonometricFunctions() {
        assertEquals(1, call(MathFunction.SIN, x).evaluate(Math.PI / 2), DELTA);
        assertEquals(-1, call(MathFunction.COS, x).evaluate(Math.PI), DELTA);
        assertEquals(1, call(MathFunction.TAN, x).evaluate(Math.PI / 4), DELTA);
        assertEquals(1, call(MathFunction.COT, x).evaluate(Math.PI / 4), DELTA);
        assertEquals(2, call(MathFunction.SEC, x).evaluate(Math.PI / 3), DELTA);
        assertEquals(2, call(MathFunction.CSC, x).evaluate(Math.PI / 6), DELTA);
    }

    @Test
    void evaluatesInverseAndHyperbolicFunctions() {
        assertEquals(Math.PI / 2, call(MathFunction.ARCSIN, x).evaluate(1), DELTA);
        assertEquals(Math.PI, call(MathFunction.ARCCOS, x).evaluate(-1), DELTA);
        assertEquals(Math.PI / 4, call(MathFunction.ARCTAN, x).evaluate(1), DELTA);
        assertEquals(0, call(MathFunction.SINH, x).evaluate(0), DELTA);
        assertEquals(1, call(MathFunction.COSH, x).evaluate(0), DELTA);
        assertEquals(0, call(MathFunction.TANH, x).evaluate(0), DELTA);
    }

    @Test
    void evaluatesExponentialLogarithmicAndOthers() {
        assertEquals(Math.E, call(MathFunction.EXP, x).evaluate(1), DELTA);
        assertEquals(1, call(MathFunction.LN, x).evaluate(Math.E), DELTA);
        assertEquals(2, call(MathFunction.LOG, x).evaluate(100), DELTA);
        assertEquals(3, call(MathFunction.SQRT, x).evaluate(9), DELTA);
        assertEquals(4, call(MathFunction.ABS, x).evaluate(-4), DELTA);
    }

    @Test
    void evaluatesCompositions() {
        // sin(x^2) en x = 2 → sin(4)
        Expression sinOfSquare = call(MathFunction.SIN, new Power(x, c(2)));
        assertEquals(Math.sin(4), sinOfSquare.evaluate(2), DELTA);

        // sin(x)*ln(x) en x = e → sin(e)
        Expression product = new Product(call(MathFunction.SIN, x), call(MathFunction.LN, x));
        assertEquals(Math.sin(Math.E), product.evaluate(Math.E), DELTA);
    }

    @Test
    void outsideDomainReturnsNaNOrInfinity() {
        assertTrue(Double.isNaN(call(MathFunction.LN, x).evaluate(-1)));
        assertTrue(Double.isNaN(call(MathFunction.SQRT, x).evaluate(-4)));
        assertTrue(Double.isNaN(call(MathFunction.ARCSIN, x).evaluate(2)));
        assertEquals(Double.NEGATIVE_INFINITY, call(MathFunction.LN, x).evaluate(0));
        assertEquals(Double.POSITIVE_INFINITY, call(MathFunction.CSC, x).evaluate(0));
    }

    @Test
    void printsFunctionCalls() {
        assertEquals("sin(x)", call(MathFunction.SIN, x).toString());
        assertEquals("sqrt(x + 1)", call(MathFunction.SQRT, new Sum(x, c(1))).toString());
        assertEquals("sin(x)^2", new Power(call(MathFunction.SIN, x), c(2)).toString());
        assertEquals("-ln(x)", new Negation(call(MathFunction.LN, x)).toString());
        assertEquals("sin(x)*ln(x)",
                new Product(call(MathFunction.SIN, x), call(MathFunction.LN, x)).toString());
        assertEquals("cos(sin(x))", call(MathFunction.COS, call(MathFunction.SIN, x)).toString());
    }

    @Test
    void findsFunctionsByNameOrAlias() {
        assertEquals(Optional.of(MathFunction.SIN), MathFunction.fromName("sin"));
        assertEquals(Optional.of(MathFunction.SIN), MathFunction.fromName("sen"));
        assertEquals(Optional.of(MathFunction.SIN), MathFunction.fromName("SEN"));
        assertEquals(Optional.of(MathFunction.ARCSIN), MathFunction.fromName("arcsen"));
        assertEquals(Optional.of(MathFunction.TAN), MathFunction.fromName("tg"));
        assertEquals(Optional.of(MathFunction.SQRT), MathFunction.fromName("raiz"));
        assertEquals(Optional.empty(), MathFunction.fromName("foo"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new FunctionCall(null, x));
        assertThrows(NullPointerException.class, () -> new FunctionCall(MathFunction.SIN, null));
    }
}
