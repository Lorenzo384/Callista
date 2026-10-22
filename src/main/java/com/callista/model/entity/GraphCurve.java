package com.callista.model.entity;

import com.callista.model.expression.Expression;

import java.util.Objects;

/**
 * Una curva que se dibuja en la gráfica.
 *
 * @param label      texto de la leyenda, por ejemplo {@code f'(x) = 6*x}
 * @param expression la función que se dibuja
 * @param style      papel de la curva (función, derivada, tangente)
 */
public record GraphCurve(String label, Expression expression, CurveStyle style) {

    public GraphCurve {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(style, "style");
    }
}
