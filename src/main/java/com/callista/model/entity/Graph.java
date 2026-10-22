package com.callista.model.entity;

import com.callista.model.graph.Range;

import java.util.List;
import java.util.Objects;

/**
 * Qué se debe dibujar como resultado de un cálculo: las curvas, los puntos
 * marcados, las áreas sombreadas, las figuras auxiliares y el intervalo de x
 * que se muestra al inicio.
 *
 * <p>Lo decide el modelo (por ejemplo, en una derivada se dibujan f, f' y la
 * recta tangente; en una integral, el área y los trapecios); la vista solo
 * lo dibuja.</p>
 *
 * @param curves   curvas, en el orden de la leyenda
 * @param points   puntos marcados
 * @param xRange   intervalo de x inicial
 * @param areas    regiones sombreadas bajo una curva
 * @param polygons figuras auxiliares, como los trapecios de un método numérico
 */
public record Graph(List<GraphCurve> curves, List<GraphPoint> points, Range xRange,
                    List<GraphArea> areas, List<GraphPolygon> polygons) {

    public Graph {
        curves = List.copyOf(curves);
        points = List.copyOf(points);
        Objects.requireNonNull(xRange, "xRange");
        areas = List.copyOf(areas);
        polygons = List.copyOf(polygons);
    }

    /**
     * Gráfica solo con curvas y puntos.
     */
    public Graph(List<GraphCurve> curves, List<GraphPoint> points, Range xRange) {
        this(curves, points, xRange, List.of(), List.of());
    }
}
