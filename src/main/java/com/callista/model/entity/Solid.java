package com.callista.model.entity;

import com.callista.model.expression.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Sólido de revolución para la vista 3D: la región del plano (uno o varios
 * tramos entre dos curvas) y la recta alrededor de la cual gira.
 *
 * <p>Lo decide el modelo; la vista calcula la superficie girando el borde de
 * cada tramo y la dibuja.</p>
 *
 * @param pieces       tramos de la región
 * @param verticalAxis si el eje es vertical ({@code x = axisValue}) u
 *                     horizontal ({@code y = axisValue})
 * @param axisValue    el valor k del eje
 * @param label        descripción breve, por ejemplo {@code V = 8π/15}
 */
public record Solid(List<Piece> pieces, boolean verticalAxis, double axisValue, String label) {

    public Solid {
        pieces = List.copyOf(pieces);
        Objects.requireNonNull(label, "label");
        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("El sólido necesita al menos un tramo");
        }
    }

    /**
     * Un tramo de la región: entre {@code lower} y {@code upper} para x en
     * [from, to].
     */
    public record Piece(double from, double to, Expression upper, Expression lower) {

        public Piece {
            Objects.requireNonNull(upper, "upper");
            Objects.requireNonNull(lower, "lower");
            if (!(from < to) || !Double.isFinite(from) || !Double.isFinite(to)) {
                throw new IllegalArgumentException("Tramo inválido: [" + from + ", " + to + "]");
            }
        }
    }
}
