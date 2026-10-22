package com.callista.model.calculus.numeric;

import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.GraphPolygon.Vertex;
import com.callista.model.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla del trapecio: une con rectas los puntos de la curva y suma las áreas
 * de los trapecios que quedan debajo.
 *
 * <pre>∫[a, b] f(x) dx ≈ Δx/2 · [f(x₀) + 2f(x₁) + … + 2f(xₙ₋₁) + f(xₙ)]</pre>
 */
public class TrapezoidalRule extends WeightedRule {

    @Override
    public String getName() {
        return "Regla del trapecio";
    }

    @Override
    public String getShortName() {
        return "Trapecio";
    }

    @Override
    public String getFormula() {
        return "Δx/2·[f(x₀) + 2f(x₁) + … + 2f(xₙ₋₁) + f(xₙ)]";
    }

    @Override
    protected int nodeCount(int n) {
        return n + 1;
    }

    @Override
    protected double nodeX(double a, double dx, int i) {
        return a + i * dx;
    }

    @Override
    protected int weight(int i, int n) {
        return i == 0 || i == n ? 1 : 2;
    }

    @Override
    protected double factor() {
        return 0.5;
    }

    @Override
    public String factorText() {
        return "Δx/2";
    }

    @Override
    public List<GraphPolygon> shapes(Expression f, double a, double b, int n) {
        double dx = (b - a) / n;
        List<GraphPolygon> shapes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double left = a + i * dx;
            double right = left + dx;
            shapes.add(fromAxis(List.of(new Vertex(left, f.evaluate(left)), new Vertex(right, f.evaluate(right)))));
        }
        return shapes;
    }
}
