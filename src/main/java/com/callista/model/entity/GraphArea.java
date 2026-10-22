package com.callista.model.entity;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;

import java.util.Objects;

/**
 * Región sombreada entre dos curvas, de {@code from} a {@code to}: el área
 * bajo una curva que representa una integral definida (la curva inferior es
 * el eje x) o el área entre dos curvas.
 *
 * @param label texto de la leyenda; las regiones con la misma etiqueta
 *              aparecen una sola vez en ella
 * @param upper la curva que limita la región por arriba
 * @param lower la curva que la limita por abajo
 * @param from  extremo izquierdo
 * @param to    extremo derecho
 */
public record GraphArea(String label, Expression upper, Expression lower, double from, double to) {

    private static final Constant X_AXIS = new Constant(0);

    public GraphArea {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(upper, "upper");
        Objects.requireNonNull(lower, "lower");
        if (!Double.isFinite(from) || !Double.isFinite(to)) {
            throw new IllegalArgumentException("Extremos no finitos: " + from + ", " + to);
        }
    }

    /**
     * Región entre la curva y el eje x.
     */
    public GraphArea(String label, Expression function, double from, double to) {
        this(label, function, X_AXIS, from, to);
    }

    /**
     * Indica si la región está limitada por el eje x (y = 0).
     */
    public boolean touchesXAxis() {
        return X_AXIS.equals(lower) || X_AXIS.equals(upper);
    }
}
