package com.callista.model.calculus.numeric;

import java.util.List;
import java.util.Optional;

/**
 * Resultado de aplicar un método numérico: todo lo necesario para explicar
 * el cálculo paso a paso.
 *
 * <p>Los tres métodos tienen la forma {@code ∫ ≈ Δx · factor · Σ pesoᵢ·f(xᵢ)}.</p>
 *
 * @param rule        el método aplicado
 * @param subintervals número de subintervalos n
 * @param dx          ancho de cada subintervalo, {@code (b - a)/n}
 * @param weightedSum {@code Σ pesoᵢ·f(xᵢ)}
 * @param value       la aproximación de la integral
 * @param nodes       puntos donde se evaluó f, con su peso
 */
public record NumericalIntegration(NumericalIntegrator rule, int subintervals, double dx,
                                   double weightedSum, double value, List<Node> nodes) {

    public NumericalIntegration {
        nodes = List.copyOf(nodes);
    }

    /**
     * Indica si f estaba definida en todos los puntos; si no, la aproximación
     * no tiene sentido.
     */
    public boolean isDefined() {
        return Double.isFinite(value);
    }

    /**
     * Primer punto donde f no está definida, si lo hay.
     */
    public Optional<Node> firstUndefinedNode() {
        return nodes.stream().filter(node -> !Double.isFinite(node.fx())).findFirst();
    }

    /**
     * Un punto de evaluación.
     *
     * @param index  índice i
     * @param x      abscisa xᵢ
     * @param fx     valor f(xᵢ)
     * @param weight peso con que entra en la suma
     */
    public record Node(int index, double x, double fx, int weight) {
    }
}
