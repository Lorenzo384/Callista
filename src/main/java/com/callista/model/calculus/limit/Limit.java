package com.callista.model.calculus.limit;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;
import com.callista.util.FormatUtils;

import java.util.Objects;

/**
 * Resultado de un límite: un número, ∞, -∞ o «no existe».
 *
 * @param kind  qué tipo de resultado es
 * @param value el valor si es finito ({@code null} en otro caso)
 * @param exact si el valor es exacto o solo una estimación numérica
 */
public record Limit(Kind kind, Expression value, boolean exact) {

    /** Tipo de resultado. */
    public enum Kind {
        FINITE, POSITIVE_INFINITY, NEGATIVE_INFINITY,
        /** No existe: la función oscila sin acercarse a un valor (sen t cuando t → ∞). */
        NONE
    }

    public static final Limit POSITIVE_INFINITY = new Limit(Kind.POSITIVE_INFINITY, null, true);
    public static final Limit NEGATIVE_INFINITY = new Limit(Kind.NEGATIVE_INFINITY, null, true);
    public static final Limit NONE = new Limit(Kind.NONE, null, true);

    public Limit {
        Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.FINITE) != (value != null)) {
            throw new IllegalArgumentException("Solo un límite finito tiene valor");
        }
    }

    public static Limit finite(Expression value) {
        return new Limit(Kind.FINITE, value, true);
    }

    /** Un valor estimado numéricamente, que no es exacto. */
    public static Limit approximate(double value) {
        return new Limit(Kind.FINITE, new Constant(Double.parseDouble(FormatUtils.formatNumber(value))), false);
    }

    /** ∞ o -∞ según el signo. */
    public static Limit infinity(double sign) {
        return sign > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
    }

    public boolean isFinite() {
        return kind == Kind.FINITE;
    }

    public boolean isInfinite() {
        return kind == Kind.POSITIVE_INFINITY || kind == Kind.NEGATIVE_INFINITY;
    }

    /** Valor numérico: el número, ±∞ o NaN si no existe. */
    public double approximation() {
        return switch (kind) {
            case FINITE -> value.evaluate(0);
            case POSITIVE_INFINITY -> Double.POSITIVE_INFINITY;
            case NEGATIVE_INFINITY -> Double.NEGATIVE_INFINITY;
            case NONE -> Double.NaN;
        };
    }

    boolean isZero() {
        return isFinite() && approximation() == 0;
    }

    /** {@code 1/2}, {@code ∞}, {@code -∞} o {@code no existe}. */
    @Override
    public String toString() {
        return switch (kind) {
            case FINITE -> exact ? value.toString() : "≈ " + value;
            case POSITIVE_INFINITY -> "∞";
            case NEGATIVE_INFINITY -> "-∞";
            case NONE -> "no existe";
        };
    }
}
