package com.callista.model.calculus.numeric;

import com.callista.model.calculus.numeric.NumericalIntegration.Node;
import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.GraphPolygon.Vertex;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * Base común de los tres métodos: todos calculan
 * {@code Δx · factor · Σ pesoᵢ·f(xᵢ)}; cada subclase solo dice dónde están
 * los puntos, qué peso tiene cada uno y cuál es el factor (patrón Template
 * Method).
 */
public abstract class WeightedRule implements NumericalIntegrator {

    /** Número mínimo de subintervalos. */
    public static final int MIN_SUBINTERVALS = 1;
    /** Número máximo de subintervalos. */
    public static final int MAX_SUBINTERVALS = 100_000;

    @Override
    public NumericalIntegration integrate(Expression f, double a, double b, int n) throws ParameterException {
        validate(n);
        double dx = (b - a) / n;
        List<Node> nodes = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < nodeCount(n); i++) {
            double x = nodeX(a, dx, i);
            double fx = f.evaluate(x);
            int weight = weight(i, n);
            nodes.add(new Node(i, x, fx, weight));
            sum += weight * fx;
        }
        double value = Double.isFinite(sum) ? dx * factor() * sum : Double.NaN;
        return new NumericalIntegration(this, n, dx, sum, value, nodes);
    }

    /**
     * Verifica que {@code n} sea válido.
     *
     * @throws ParameterException si no lo es, con un mensaje para el estudiante
     */
    protected void validate(int n) throws ParameterException {
        if (n < MIN_SUBINTERVALS || n > MAX_SUBINTERVALS) {
            throw new ParameterException("El número de subintervalos debe estar entre "
                    + MIN_SUBINTERVALS + " y " + MAX_SUBINTERVALS);
        }
    }

    /** Cuántos puntos se evalúan con {@code n} subintervalos. */
    protected abstract int nodeCount(int n);

    /** Abscisa del punto {@code i}. */
    protected abstract double nodeX(double a, double dx, int i);

    /** Peso del punto {@code i} en la suma. */
    protected abstract int weight(int i, int n);

    /** Factor que multiplica a {@code Δx}: 1/2, 1/3 o 1. */
    protected abstract double factor();

    /**
     * Cómo se escribe {@code Δx · factor}: {@code Δx/2}, {@code Δx/3} o {@code Δx}.
     */
    public abstract String factorText();

    /**
     * Símbolo de los puntos en el procedimiento: {@code x} (extremos) o
     * {@code m} (puntos medios).
     */
    public String nodeSymbol() {
        return "x";
    }

    /** Polígono entre el eje x y los puntos dados, para dibujar una figura. */
    protected static GraphPolygon fromAxis(List<Vertex> top) {
        List<Vertex> vertices = new ArrayList<>();
        vertices.add(new Vertex(top.getFirst().x(), 0));
        vertices.addAll(top);
        vertices.add(new Vertex(top.getLast().x(), 0));
        return new GraphPolygon(vertices);
    }
}
