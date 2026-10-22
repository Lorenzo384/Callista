package com.callista.model.entity;

import java.util.Objects;

/**
 * Un punto marcado en la gráfica, por ejemplo {@code (2, 13)}.
 *
 * @param label texto junto al punto
 * @param x     abscisa
 * @param y     ordenada
 */
public record GraphPoint(String label, double x, double y) {

    public GraphPoint {
        Objects.requireNonNull(label, "label");
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Punto no finito: (" + x + ", " + y + ")");
        }
    }
}
