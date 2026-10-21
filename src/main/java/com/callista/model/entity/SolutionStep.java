package com.callista.model.entity;

import java.util.Objects;

/**
 * Un paso del procedimiento que se muestra al estudiante,
 * por ejemplo «Sustituir x = 2».
 *
 * <p>El nivel indica de qué paso depende: un paso de nivel 1 detalla al paso
 * de nivel 0 anterior (por ejemplo, la derivada de un término dentro de la
 * regla de la suma). La vista lo muestra con sangría.</p>
 */
public final class SolutionStep {

    private final String description;
    private final int level;

    public SolutionStep(String description) {
        this(description, 0);
    }

    public SolutionStep(String description, int level) {
        this.description = Objects.requireNonNull(description, "description");
        if (level < 0) {
            throw new IllegalArgumentException("El nivel no puede ser negativo: " + level);
        }
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Profundidad del paso: 0 para los pasos principales.
     */
    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return description;
    }
}
