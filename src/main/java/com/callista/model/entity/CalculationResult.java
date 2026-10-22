package com.callista.model.entity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resultado de un cálculo: el resumen que se muestra en grande
 * (por ejemplo {@code f(2) = 13}), los pasos del procedimiento y, si
 * corresponde, la gráfica.
 */
public final class CalculationResult {

    private final String summary;
    private final List<SolutionStep> steps;
    private final Graph graph;

    public CalculationResult(String summary, List<SolutionStep> steps) {
        this(summary, steps, null);
    }

    /**
     * @param graph lo que se dibuja en la pestaña «Gráfica»
     */
    public CalculationResult(String summary, List<SolutionStep> steps, Graph graph) {
        this.summary = Objects.requireNonNull(summary, "summary");
        this.steps = List.copyOf(steps);
        this.graph = graph;
    }

    public String getSummary() {
        return summary;
    }

    /**
     * Pasos en orden; la lista no se puede modificar.
     */
    public List<SolutionStep> getSteps() {
        return steps;
    }

    /**
     * La gráfica del resultado, si la tiene.
     */
    public Optional<Graph> getGraph() {
        return Optional.ofNullable(graph);
    }
}
