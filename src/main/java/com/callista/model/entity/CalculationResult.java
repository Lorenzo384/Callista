package com.callista.model.entity;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de un cálculo: el resumen que se muestra en grande
 * (por ejemplo {@code f(2) = 13}) y los pasos del procedimiento.
 */
public final class CalculationResult {

    private final String summary;
    private final List<SolutionStep> steps;

    public CalculationResult(String summary, List<SolutionStep> steps) {
        this.summary = Objects.requireNonNull(summary, "summary");
        this.steps = List.copyOf(steps);
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
}
