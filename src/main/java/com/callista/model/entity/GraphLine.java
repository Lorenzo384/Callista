package com.callista.model.entity;

import java.util.Objects;

/**
 * Recta vertical {@code x = c} que se dibuja en la gráfica, como un eje de
 * giro. (Las rectas horizontales se dibujan como curvas constantes.)
 *
 * @param label texto de la leyenda
 * @param x     dónde corta al eje x
 */
public record GraphLine(String label, double x) {

    public GraphLine {
        Objects.requireNonNull(label, "label");
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("Recta no finita: x = " + x);
        }
    }
}
