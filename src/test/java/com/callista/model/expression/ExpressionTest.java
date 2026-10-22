package com.callista.model.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest {

    private static final double DELTA = 1e-9;

    private final Variable x = new Variable("x");

    private static Constant c(double value) {
        return new Constant(value);
    }

    /** 3*x^2 + 1 */
    private Expression polynomial() {
        return new Sum(new Product(c(3), new Power(x, c(2))), c(1));
    }

    @Test
    void evaluatesPolynomial() {
        assertEquals(13, polynomial().evaluate(2), DELTA);
        assertEquals(1, polynomial().evaluate(0), DELTA);
    }

    @Test
    void evaluatesEachOperation() {
        assertEquals(7, new Sum(x, c(2)).evaluate(5), DELTA);
        assertEquals(3, new Difference(x, c(2)).evaluate(5), DELTA);
        assertEquals(10, new Product(x, c(2)).evaluate(5), DELTA);
        assertEquals(2.5, new Quotient(x, c(2)).evaluate(5), DELTA);
        assertEquals(25, new Power(x, c(2)).evaluate(5), DELTA);
        assertEquals(-5, new Negation(x).evaluate(5), DELTA);
    }

    @Test
    void evaluatesConstants() {
        assertEquals(Math.PI, Constant.PI.evaluate(0), DELTA);
        assertEquals(Math.E, Constant.E.evaluate(0), DELTA);
    }

    @Test
    void undefinedOperationsReturnInfinityOrNaN() {
        assertEquals(Double.POSITIVE_INFINITY, new Quotient(c(1), x).evaluate(0));
        assertTrue(Double.isNaN(new Power(x, c(0.5)).evaluate(-1)));
    }

    @Test
    void printsWithMinimalParentheses() {
        assertEquals("3*x^2 + 1", polynomial().toString());
        assertEquals("(x + 1)*x", new Product(new Sum(x, c(1)), x).toString());
        assertEquals("x*(x + 1)", new Product(x, new Sum(x, c(1))).toString());
        assertEquals("x + 1 + 2", new Sum(new Sum(x, c(1)), c(2)).toString());
    }

    @Test
    void keepsParenthesesThatChangeTheResult() {
        assertEquals("x - (x + 1)", new Difference(x, new Sum(x, c(1))).toString());
        assertEquals("x/(x*2)", new Quotient(x, new Product(x, c(2))).toString());
        assertEquals("x/2/3", new Quotient(new Quotient(x, c(2)), c(3)).toString());
        assertEquals("x/(2/3)", new Quotient(x, new Quotient(c(2), c(3))).toString());
    }

    @Test
    void omitsParenthesesForAssociativeOperations() {
        // a*(b*c) = a*b*c y a + (b - c) = a + b - c
        assertEquals("x*2*x", new Product(x, new Product(c(2), x)).toString());
        assertEquals("x + x - 1", new Sum(x, new Difference(x, c(1))).toString());
        // pero a - (b - c) y a/(b/c) sí los necesitan
        assertEquals("x - (x - 1)", new Difference(x, new Difference(x, c(1))).toString());
    }

    @Test
    void knowsWhetherItDependsOnTheVariable() {
        assertTrue(x.dependsOnVariable());
        assertTrue(new Sum(c(1), new Power(x, c(2))).dependsOnVariable());
        assertTrue(new FunctionCall(MathFunction.SIN, x).dependsOnVariable());
        assertFalse(c(3).dependsOnVariable());
        assertFalse(new Power(Constant.PI, c(2)).dependsOnVariable());
        assertFalse(new FunctionCall(MathFunction.LN, c(2)).dependsOnVariable());
        assertFalse(new Negation(Constant.E).dependsOnVariable());
    }

    @Test
    void replacesAndFindsSubexpressions() {
        Expression f = new Product(x, new FunctionCall(MathFunction.COS, new Power(x, c(2))));
        Variable u = new Variable("u");
        assertEquals("x*cos(u)", f.replace(new Power(x, c(2)), u).toString());
        assertEquals("2*cos(2^2)", f.replace(x, c(2)).toString());
        assertTrue(f.contains(new Power(x, c(2))));
        assertFalse(f.contains(u));
    }

    @Test
    void comparesStructurally() {
        assertEquals(new Power(x, c(2)), new Power(new Variable("x"), c(2)));
        assertEquals(new Power(x, c(2)).hashCode(), new Power(new Variable("x"), c(2)).hashCode());
        assertEquals(new FunctionCall(MathFunction.SIN, x), new FunctionCall(MathFunction.SIN, x));
        assertEquals(c(0), c(-0.0));
        assertNotEquals(new Sum(x, c(1)), new Sum(c(1), x));
        assertNotEquals(new Sum(x, c(1)), new Product(x, c(1)));
        assertNotEquals(Constant.PI, c(Math.PI));
    }

    @Test
    void powerIsRightAssociative() {
        assertEquals("2^(3^2)", new Power(c(2), new Power(c(3), c(2))).toString());
        assertEquals("(2^3)^2", new Power(new Power(c(2), c(3)), c(2)).toString());
        assertEquals(512, new Power(c(2), new Power(c(3), c(2))).evaluate(0), DELTA);
    }

    @Test
    void printsNegatives() {
        assertEquals("-x^2", new Negation(new Power(x, c(2))).toString());
        assertEquals("(-x)^2", new Power(new Negation(x), c(2)).toString());
        assertEquals("-(x + 1)", new Negation(new Sum(x, c(1))).toString());
        assertEquals("-(-x)", new Negation(new Negation(x)).toString());
        assertEquals("x*(-2)", new Product(x, c(-2)).toString());
        assertEquals("(-2)^x", new Power(c(-2), x).toString());
    }

    @Test
    void printsConstants() {
        assertEquals("3", c(3).toString());
        assertEquals("0.5", c(0.5).toString());
        assertEquals("2*π", new Product(c(2), Constant.PI).toString());
        assertEquals("e^x", new Power(Constant.E, x).toString());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Variable(" "));
        assertThrows(NullPointerException.class, () -> new Sum(x, null));
    }
}
