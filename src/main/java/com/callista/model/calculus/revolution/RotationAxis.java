package com.callista.model.calculus.revolution;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;

import java.util.Objects;

/**
 * Recta alrededor de la cual gira la región: el eje x ({@code y = 0}), el
 * eje y ({@code x = 0}), una recta horizontal {@code y = k} o una vertical
 * {@code x = k}.
 *
 * @param vertical si la recta es vertical (x = k) u horizontal (y = k)
 * @param k        el valor de la recta, exacto
 */
public record RotationAxis(boolean vertical, Expression k) {

    public RotationAxis {
        Objects.requireNonNull(k, "k");
        if (k.dependsOnVariable() || !Double.isFinite(k.evaluate(0))) {
            throw new IllegalArgumentException("k debe ser un número: " + k);
        }
    }

    public static RotationAxis xAxis() {
        return new RotationAxis(false, new Constant(0));
    }

    public static RotationAxis yAxis() {
        return new RotationAxis(true, new Constant(0));
    }

    public static RotationAxis horizontalLine(Expression k) {
        return new RotationAxis(false, k);
    }

    public static RotationAxis verticalLine(Expression k) {
        return new RotationAxis(true, k);
    }

    public double value() {
        return k.evaluate(0);
    }

    /** {@code y = 2} o {@code x = -1}. */
    public String equation() {
        return (vertical ? "x = " : "y = ") + k;
    }

    /** {@code el eje x}, {@code el eje y} o {@code la recta y = 2}. */
    @Override
    public String toString() {
        if (value() == 0) {
            return vertical ? "el eje y (x = 0)" : "el eje x (y = 0)";
        }
        return "la recta " + equation();
    }
}
