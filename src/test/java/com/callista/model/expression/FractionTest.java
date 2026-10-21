package com.callista.model.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractionTest {

    @Test
    void isAlwaysReducedWithPositiveDenominator() {
        assertEquals("1/2", Fraction.of(2, 4).toString());
        assertEquals("-2/3", Fraction.of(4, -6).toString());
        assertEquals("3", Fraction.of(6, 2).toString());
        assertEquals("0", Fraction.of(0, 5).toString());
    }

    @Test
    void operatesExactly() {
        assertEquals(Fraction.of(1, 2), Fraction.of(1, 3).add(Fraction.of(1, 6)));
        assertEquals(Fraction.of(2, 9), Fraction.of(2, 3).multiply(Fraction.of(1, 3)));
        assertEquals(Fraction.of(-3, 4), Fraction.of(3, 4).negate());
        assertEquals(Fraction.of(4, 3), Fraction.of(3, 4).reciprocal());
        assertEquals(Fraction.of(9, 4), Fraction.of(2, 3).pow(-2));
        assertEquals(Fraction.ONE, Fraction.of(5).pow(0));
    }

    @Test
    void convertsWrittenDecimals() {
        assertEquals(Fraction.of(1, 2), Fraction.fromDouble(0.5));
        assertEquals(Fraction.of(5, 4), Fraction.fromDouble(1.25));
        assertEquals(Fraction.of(1, 10), Fraction.fromDouble(0.1));
        assertEquals(Fraction.of(-3), Fraction.fromDouble(-3.0));
        assertEquals(Fraction.of(1500), Fraction.fromDouble(1.5e3));
    }

    @Test
    void answersQuestionsAboutItself() {
        assertTrue(Fraction.ZERO.isZero());
        assertTrue(Fraction.of(3, 3).isOne());
        assertTrue(Fraction.of(4, 2).isInteger());
        assertFalse(Fraction.of(1, 2).isInteger());
        assertEquals(-1, Fraction.of(-1, 2).signum());
        assertEquals(0.25, Fraction.of(1, 4).doubleValue());
    }

    @Test
    void becomesAnExpression() {
        assertEquals("3", Fraction.of(3).toExpression().toString());
        assertEquals("2/3", Fraction.of(2, 3).toExpression().toString());
        assertEquals("-1/2", Fraction.of(-1, 2).toExpression().toString());
    }

    @Test
    void reportsImpossibleOperations() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.ZERO.reciprocal());
        assertThrows(ArithmeticException.class, () -> Fraction.fromDouble(Double.NaN));
        assertThrows(ArithmeticException.class, () -> Fraction.of(Long.MAX_VALUE).add(Fraction.ONE));
    }
}
