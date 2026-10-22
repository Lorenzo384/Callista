package com.callista.model.calculus.numeric;

import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.GraphPolygon.Vertex;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla de Simpson 1/3: en cada par de subintervalos aproxima la curva con
 * la parábola que pasa por sus tres puntos. Por eso necesita un número par
 * de subintervalos y suele ser el más preciso de los tres.
 *
 * <pre>∫[a, b] f(x) dx ≈ Δx/3 · [f(x₀) + 4f(x₁) + 2f(x₂) + 4f(x₃) + … + 4f(xₙ₋₁) + f(xₙ)]</pre>
 */
public class SimpsonRule extends WeightedRule {

    /** Puntos con que se dibuja cada parábola. */
    private static final int PARABOLA_POINTS = 16;

    @Override
    public String getName() {
        return "Regla de Simpson 1/3";
    }

    @Override
    public String getShortName() {
        return "Simpson 1/3";
    }

    @Override
    public String getFormula() {
        return "Δx/3·[f(x₀) + 4f(x₁) + 2f(x₂) + 4f(x₃) + … + 4f(xₙ₋₁) + f(xₙ)]";
    }

    @Override
    protected void validate(int n) throws ParameterException {
        super.validate(n);
        if (n % 2 != 0) {
            throw new ParameterException("La regla de Simpson necesita un número par de subintervalos: prueba con n = "
                    + (n + 1));
        }
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
        if (i == 0 || i == n) {
            return 1;
        }
        return i % 2 == 1 ? 4 : 2;
    }

    @Override
    protected double factor() {
        return 1.0 / 3;
    }

    @Override
    public String factorText() {
        return "Δx/3";
    }

    /**
     * Una figura por cada par de subintervalos, limitada arriba por la
     * parábola que pasa por (x₀, y₀), (x₁, y₁), (x₂, y₂).
     */
    @Override
    public List<GraphPolygon> shapes(Expression f, double a, double b, int n) {
        double dx = (b - a) / n;
        List<GraphPolygon> shapes = new ArrayList<>();
        for (int i = 0; i + 2 <= n; i += 2) {
            double x0 = a + i * dx;
            double y0 = f.evaluate(x0);
            double y1 = f.evaluate(x0 + dx);
            double y2 = f.evaluate(x0 + 2 * dx);
            List<Vertex> top = new ArrayList<>();
            for (int k = 0; k <= PARABOLA_POINTS; k++) {
                double t = 2.0 * k / PARABOLA_POINTS; // t = (x - x0)/dx, de 0 a 2
                // Interpolación de Lagrange con nodos t = 0, 1, 2
                double y = y0 * (t - 1) * (t - 2) / 2 - y1 * t * (t - 2) + y2 * t * (t - 1) / 2;
                top.add(new Vertex(x0 + t * dx, y));
            }
            shapes.add(fromAxis(top));
        }
        return shapes;
    }
}
