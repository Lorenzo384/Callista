package com.callista.model.calculus.limit;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;

import java.util.Objects;

/**
 * Hacia dónde tiende la variable: a ∞, a -∞ o a un número c por un lado
 * (c⁺ por la derecha, c⁻ por la izquierda).
 *
 * @param point el número c, exacto ({@code π/2}); se ignora si es infinito
 * @param kind  a qué tiende
 */
public record LimitPoint(Expression point, Kind kind) {

    /** A qué tiende la variable. */
    public enum Kind {
        POSITIVE_INFINITY, NEGATIVE_INFINITY, FROM_RIGHT, FROM_LEFT
    }

    public LimitPoint {
        Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.FROM_RIGHT || kind == Kind.FROM_LEFT) && point == null) {
            throw new IllegalArgumentException("Falta el punto");
        }
    }

    public static LimitPoint positiveInfinity() {
        return new LimitPoint(Constant.INFINITY, Kind.POSITIVE_INFINITY);
    }

    public static LimitPoint negativeInfinity() {
        return new LimitPoint(Constant.INFINITY, Kind.NEGATIVE_INFINITY);
    }

    public static LimitPoint fromRight(Expression c) {
        return new LimitPoint(c, Kind.FROM_RIGHT);
    }

    public static LimitPoint fromLeft(Expression c) {
        return new LimitPoint(c, Kind.FROM_LEFT);
    }

    public boolean isInfinite() {
        return kind == Kind.POSITIVE_INFINITY || kind == Kind.NEGATIVE_INFINITY;
    }

    /**
     * Un punto cada vez más cercano al destino según {@code k}: 10^k hacia
     * ∞, o c ± 10^-k hacia c.
     */
    public double near(int k) {
        return switch (kind) {
            case POSITIVE_INFINITY -> Math.pow(10, k);
            case NEGATIVE_INFINITY -> -Math.pow(10, k);
            case FROM_RIGHT -> point.evaluate(0) + Math.pow(10, -k);
            case FROM_LEFT -> point.evaluate(0) - Math.pow(10, -k);
        };
    }

    /**
     * El punto {@link #near(int)} escrito para una tabla: {@code 1000},
     * {@code -10^6}, {@code 10^-4}, {@code π/2 - 10^-4}.
     */
    public String nearText(int k) {
        String power = k <= 3 ? Long.toString((long) Math.pow(10, k)) : "10^" + k;
        String small = "10^-" + k;
        boolean atZero = !isInfinite() && point.evaluate(0) == 0;
        return switch (kind) {
            case POSITIVE_INFINITY -> power;
            case NEGATIVE_INFINITY -> "-" + power;
            case FROM_RIGHT -> atZero ? small : point + " + " + small;
            case FROM_LEFT -> atZero ? "-" + small : point + " - " + small;
        };
    }

    /** {@code t → ∞}, {@code t → -∞}, {@code t → 0⁺}, {@code t → 1⁻}. */
    public String describe(String variable) {
        return variable + " → " + switch (kind) {
            case POSITIVE_INFINITY -> "∞";
            case NEGATIVE_INFINITY -> "-∞";
            case FROM_RIGHT -> point + "⁺";
            case FROM_LEFT -> point + "⁻";
        };
    }
}
