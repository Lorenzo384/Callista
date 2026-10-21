package com.callista.model.expression;

import org.junit.jupiter.api.Test;

import static com.callista.model.expression.Simplifier.difference;
import static com.callista.model.expression.Simplifier.negate;
import static com.callista.model.expression.Simplifier.power;
import static com.callista.model.expression.Simplifier.product;
import static com.callista.model.expression.Simplifier.quotient;
import static com.callista.model.expression.Simplifier.sum;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimplifierTest {

    private final Variable x = new Variable("x");

    private static Constant c(double value) {
        return new Constant(value);
    }

    private static String text(Expression expression) {
        return expression.toString();
    }

    @Test
    void removesNeutralElements() {
        assertEquals("x", text(sum(x, c(0))));
        assertEquals("x", text(sum(c(0), x)));
        assertEquals("x", text(difference(x, c(0))));
        assertEquals("x", text(product(c(1), x)));
        assertEquals("x", text(quotient(x, c(1))));
        assertEquals("x", text(power(x, c(1))));
    }

    @Test
    void appliesAbsorbingElements() {
        assertEquals("0", text(product(x, c(0))));
        assertEquals("0", text(quotient(c(0), x)));
        assertEquals("1", text(power(x, c(0))));
        assertEquals("1", text(power(c(1), x)));
    }

    @Test
    void operatesNumbers() {
        assertEquals("5", text(sum(c(2), c(3))));
        assertEquals("-1", text(difference(c(2), c(3))));
        assertEquals("6", text(product(c(2), c(3))));
        assertEquals("3", text(quotient(c(6), c(2))));
        assertEquals("1/3", text(quotient(c(1), c(3))));
        assertEquals("8", text(power(c(2), c(3))));
    }

    @Test
    void gathersCoefficientsInFront() {
        assertEquals("3*x", text(product(x, c(3))));
        assertEquals("6*x", text(product(c(3), product(c(2), x))));
        assertEquals("6*x*x^2", text(product(product(c(2), x), product(c(3), power(x, c(2))))));
        assertEquals("2*π*x", text(product(Constant.PI, product(c(2), x))));
    }

    @Test
    void writesSignsNaturally() {
        assertEquals("x - 2", text(sum(x, c(-2))));
        assertEquals("x - x^2", text(sum(x, new Negation(power(x, c(2))))));
        assertEquals("x - 1/x^2", text(sum(x, quotient(c(-1), power(x, c(2))))));
        assertEquals("x + 2", text(difference(x, c(-2))));
        assertEquals("x", text(negate(new Negation(x))));
        assertEquals("-x", text(product(c(-1), x)));
        assertEquals("-6*x", text(negate(product(c(6), x))));
        assertEquals("-x^2", text(negate(power(x, c(2)))));
    }

    @Test
    void treatsAWrittenMinusTwoAsTheNumberMinusTwo() {
        // El parser escribe -2 como Negation(2)
        assertEquals("-3", text(difference(new Negation(c(2)), c(1))));
        assertEquals("x^(-3)", text(power(x, difference(new Negation(c(2)), c(1)))));
    }

    @Test
    void keepsNamedConstants() {
        assertEquals("π + 1", text(sum(Constant.PI, c(1))));
        assertEquals("e^x", text(power(Constant.E, x)));
    }
}
