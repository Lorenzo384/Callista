package com.callista.model.entity;

import com.callista.model.graph.Range;

import java.util.List;
import java.util.Objects;

/**
 * Qué se debe dibujar como resultado de un cálculo: las curvas, los puntos
 * marcados y el intervalo de x que se muestra al inicio.
 *
 * <p>Lo decide el modelo (por ejemplo, en una derivada se dibujan f, f' y la
 * recta tangente); la vista solo lo dibuja.</p>
 *
 * @param curves curvas, en el orden de la leyenda
 * @param points puntos marcados
 * @param xRange intervalo de x inicial
 */
public record Graph(List<GraphCurve> curves, List<GraphPoint> points, Range xRange) {

    public Graph {
        curves = List.copyOf(curves);
        points = List.copyOf(points);
        Objects.requireNonNull(xRange, "xRange");
    }
}
