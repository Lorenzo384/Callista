package com.callista.model.calculus.numeric;

import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.GraphPolygon.Vertex;
import com.callista.model.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla del punto medio: en cada subintervalo, un rectángulo cuya altura es
 * el valor de f en el punto medio.
 *
 * <pre>∫[a, b] f(x) dx ≈ Δx · [f(m₀) + f(m₁) + … + f(mₙ₋₁)],  mᵢ = a + (i + 1/2)·Δx</pre>
 */
public class MidpointRule extends WeightedRule {

    @Override
    public String getName() {
        return "Regla del punto medio";
    }

    @Override
    public String getShortName() {
        return "Punto medio";
    }

    @Override
    public String getFormula() {
        return "Δx·[f(m₀) + f(m₁) + … + f(mₙ₋₁)], con mᵢ = a + (i + 1/2)·Δx";
    }

    @Override
    protected int nodeCount(int n) {
        return n;
    }

    @Override
    protected double nodeX(double a, double dx, int i) {
        return a + (i + 0.5) * dx;
    }

    @Override
    protected int weight(int i, int n) {
        return 1;
    }

    @Override
    protected double factor() {
        return 1;
    }

    @Override
    public String factorText() {
        return "Δx";
    }

    @Override
    public String nodeSymbol() {
        return "m";
    }

    @Override
    public List<GraphPolygon> shapes(Expression f, double a, double b, int n) {
        double dx = (b - a) / n;
        List<GraphPolygon> shapes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double left = a + i * dx;
            double right = left + dx;
            double height = f.evaluate(left + dx / 2);
            shapes.add(fromAxis(List.of(new Vertex(left, height), new Vertex(right, height))));
        }
        return shapes;
    }
}
