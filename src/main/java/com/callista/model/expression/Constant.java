package com.callista.model.expression;

import java.util.Objects;

/**
 * Número fijo, como {@code 3}, {@code 0.5}, {@code π} o {@code e}.
 */
public final class Constant implements Expression {

    /** La constante π ≈ 3.14159. */
    public static final Constant PI = new Constant(Math.PI, "π");
    /** La constante de Euler e ≈ 2.71828. */
    public static final Constant E = new Constant(Math.E, "e");

    private final double value;
    private final String symbol;

    public Constant(double value) {
        this(value, null);
    }

    private Constant(double value, String symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public double getValue() {
        return value;
    }

    /**
     * Indica si es un número escrito con dígitos, y no una constante con
     * nombre como {@code π} o {@code e}. Solo los números se pueden operar
     * entre sí sin perder cómo se escribieron.
     */
    public boolean isNumber() {
        return symbol == null;
    }

    /**
     * Indica si es exactamente el número {@code expected}.
     */
    public boolean isNumber(double expected) {
        return isNumber() && value == expected;
    }

    @Override
    public double evaluate(double x) {
        return value;
    }

    @Override
    public boolean dependsOnVariable() {
        return false;
    }

    /**
     * Un número negativo se comporta como un signo negativo al escribirlo:
     * {@code (-2)^x} necesita paréntesis igual que {@code (-a)^x}.
     */
    @Override
    public int precedence() {
        return value < 0 ? UNARY : ATOM;
    }

    @Override
    public boolean equals(Object other) {
        // + 0.0 hace que -0.0 y 0.0 se consideren iguales
        return other instanceof Constant c
                && Double.compare(value + 0.0, c.value + 0.0) == 0
                && Objects.equals(symbol, c.symbol);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value + 0.0) * 31 + Objects.hashCode(symbol);
    }

    /**
     * Los enteros se muestran sin decimales ({@code 3} y no {@code 3.0}).
     */
    @Override
    public String toString() {
        if (symbol != null) {
            return symbol;
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
