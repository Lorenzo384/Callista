package com.callista.model.expression;

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

    @Override
    public double evaluate(double x) {
        return value;
    }

    /**
     * Un número negativo se comporta como un signo negativo al escribirlo:
     * {@code (-2)^x} necesita paréntesis igual que {@code (-a)^x}.
     */
    @Override
    public int precedence() {
        return value < 0 ? UNARY : ATOM;
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
