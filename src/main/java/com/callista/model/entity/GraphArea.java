package com.callista.model.entity;

import com.callista.model.expression.Expression;

import java.util.Objects;

/**
 * Región sombreada entre la curva y el eje x, de {@code from} a {@code to}:
 * el área que representa una integral definida.
 *
 * @param label    texto de la leyenda
 * @param function la curva que limita la región
 * @param from     extremo izquierdo
 * @param to       extremo derecho
 */
public record GraphArea(String label, Expression function, double from, double to) {

    public GraphArea {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(function, "function");
        if (!Double.isFinite(from) || !Double.isFinite(to)) {
            throw new IllegalArgumentException("Extremos no finitos: " + from + ", " + to);
        }
    }
}
