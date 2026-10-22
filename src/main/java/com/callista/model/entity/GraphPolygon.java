package com.callista.model.entity;

import java.util.List;

/**
 * Figura que se dibuja sobre la gráfica, como cada trapecio o rectángulo de
 * un método de integración numérica.
 *
 * @param vertices vértices en coordenadas de la gráfica, en orden
 */
public record GraphPolygon(List<Vertex> vertices) {

    public GraphPolygon {
        vertices = List.copyOf(vertices);
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("Un polígono necesita al menos 3 vértices");
        }
    }

    /**
     * Un vértice {@code (x, y)}.
     */
    public record Vertex(double x, double y) {
    }
}
