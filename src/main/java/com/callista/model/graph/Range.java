package com.callista.model.graph;

/**
 * Intervalo cerrado {@code [min, max]} de un eje de la gráfica.
 *
 * @param min extremo inferior
 * @param max extremo superior, mayor que {@code min}
 */
public record Range(double min, double max) {

    public Range {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min >= max) {
            throw new IllegalArgumentException("Intervalo inválido: [" + min + ", " + max + "]");
        }
    }

    /**
     * Intervalo de ancho {@code width} centrado en {@code center}.
     */
    public static Range centeredAt(double center, double width) {
        return new Range(center - width / 2, center + width / 2);
    }

    public double width() {
        return max - min;
    }

    public double center() {
        return (min + max) / 2;
    }

    public boolean contains(double value) {
        return value >= min && value <= max;
    }

    /**
     * Agranda (factor &gt; 1) o achica (factor &lt; 1) el intervalo manteniendo
     * fijo el punto {@code anchor}: es el zoom alrededor del cursor.
     */
    public Range scale(double factor, double anchor) {
        return new Range(anchor - (anchor - min) * factor, anchor + (max - anchor) * factor);
    }

    public Range shift(double delta) {
        return new Range(min + delta, max + delta);
    }
}
