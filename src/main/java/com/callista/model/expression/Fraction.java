package com.callista.model.expression;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Número racional exacto {@code numerador/denominador}, siempre reducido y con
 * denominador positivo.
 *
 * <p>El simplificador lo usa para operar coeficientes y exponentes sin perder
 * precisión: {@code 1/3 + 1/6 = 1/2}, mientras que con {@code double} se
 * obtendría {@code 0.49999999999999994}.</p>
 *
 * <p>Si un resultado no cabe en un {@code long} se lanza
 * {@link ArithmeticException}; el simplificador la captura y deja la
 * expresión como estaba.</p>
 */
public final class Fraction {

    public static final Fraction ZERO = new Fraction(0, 1);
    public static final Fraction ONE = new Fraction(1, 1);

    private final long numerator;
    private final long denominator;

    private Fraction(long numerator, long denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Fraction of(long integer) {
        return new Fraction(integer, 1);
    }

    /**
     * @throws ArithmeticException si el denominador es 0
     */
    public static Fraction of(long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("División entre cero");
        }
        if (denominator < 0) {
            numerator = Math.negateExact(numerator);
            denominator = Math.negateExact(denominator);
        }
        long gcd = gcd(Math.absExact(numerator), denominator);
        return new Fraction(numerator / gcd, denominator / gcd);
    }

    /**
     * Convierte un decimal escrito por el usuario a fracción exacta:
     * {@code 0.5 → 1/2}, {@code 1.25 → 5/4}.
     *
     * @throws ArithmeticException si el valor no es finito o es demasiado grande
     */
    public static Fraction fromDouble(double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("Valor no finito: " + value);
        }
        BigDecimal decimal = new BigDecimal(Double.toString(value)).stripTrailingZeros();
        BigInteger unscaled = decimal.unscaledValue();
        int scale = decimal.scale();
        if (scale <= 0) {
            return of(unscaled.multiply(BigInteger.TEN.pow(-scale)).longValueExact());
        }
        return of(unscaled.longValueExact(), BigInteger.TEN.pow(scale).longValueExact());
    }

    public long getNumerator() {
        return numerator;
    }

    public long getDenominator() {
        return denominator;
    }

    public Fraction add(Fraction other) {
        return of(Math.addExact(Math.multiplyExact(numerator, other.denominator),
                        Math.multiplyExact(other.numerator, denominator)),
                Math.multiplyExact(denominator, other.denominator));
    }

    public Fraction multiply(Fraction other) {
        return of(Math.multiplyExact(numerator, other.numerator),
                Math.multiplyExact(denominator, other.denominator));
    }

    public Fraction negate() {
        return new Fraction(Math.negateExact(numerator), denominator);
    }

    /**
     * @throws ArithmeticException si la fracción es 0
     */
    public Fraction reciprocal() {
        return of(denominator, numerator);
    }

    /**
     * Potencia entera, también negativa: {@code (2/3)^-2 = 9/4}.
     */
    public Fraction pow(long exponent) {
        Fraction base = exponent < 0 ? reciprocal() : this;
        Fraction result = ONE;
        for (long i = 0; i < Math.absExact(exponent); i++) {
            result = result.multiply(base);
        }
        return result;
    }

    public boolean isZero() {
        return numerator == 0;
    }

    public boolean isOne() {
        return numerator == 1 && denominator == 1;
    }

    public boolean isInteger() {
        return denominator == 1;
    }

    /**
     * -1, 0 o 1 según el signo.
     */
    public int signum() {
        return Long.signum(numerator);
    }

    public double doubleValue() {
        return (double) numerator / denominator;
    }

    /**
     * Expresión equivalente: {@code 3}, {@code -2} o {@code 2/3}.
     */
    public Expression toExpression() {
        Constant top = new Constant(numerator);
        return isInteger() ? top : new Quotient(top, new Constant(denominator));
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long rest = a % b;
            a = b;
            b = rest;
        }
        return a == 0 ? 1 : a;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Fraction f && numerator == f.numerator && denominator == f.denominator;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(numerator) * 31 + Long.hashCode(denominator);
    }

    @Override
    public String toString() {
        return isInteger() ? Long.toString(numerator) : numerator + "/" + denominator;
    }
}
