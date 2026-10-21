package com.callista.model.entity;

import java.util.Objects;

/**
 * Un paso del procedimiento que se muestra al estudiante,
 * por ejemplo «Sustituir x = 2».
 */
public final class SolutionStep {

    private final String description;

    public SolutionStep(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
