package com.callista.model.graph;

import com.callista.model.expression.Expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Evalúa funciones en muchos puntos para dibujarlas.
 */
public final class FunctionSampler {

    /** Altura mínima del eje y, para que una función constante no se vea como una línea sin escala. */
    private static final double MIN_HEIGHT = 2;
    /** Puntos que se usan para estimar la escala vertical. */
    private static final int SCALE_SAMPLES = 400;

    private FunctionSampler() {
    }

    /**
     * Un punto de la curva; {@code y} es {@code NaN} donde la función no está
     * definida.
     */
    public record Sample(double x, double y) {
    }

    /**
     * Evalúa la función en {@code count} puntos igualmente espaciados del
     * intervalo, extremos incluidos.
     */
    public static List<Sample> sample(Expression function, Range range, int count) {
        if (count < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 puntos: " + count);
        }
        List<Sample> samples = new ArrayList<>(count);
        double step = range.width() / (count - 1);
        for (int i = 0; i < count; i++) {
            double x = range.min() + i * step;
            double y = function.evaluate(x);
            samples.add(new Sample(x, Double.isFinite(y) ? y : Double.NaN));
        }
        return samples;
    }

    /**
     * Escala vertical sugerida para ver bien las funciones en el intervalo.
     *
     * <p>Ignora los valores atípicos para que una asíntota (como las de
     * {@code tan(x)} o {@code 1/x}) no aplaste el resto de la gráfica: se
     * descarta el 2 % de los valores de cada extremo y lo que quede a más de
     * tres rangos intercuartílicos del centro. Luego deja un margen del 10 %
     * e incluye el eje x si está cerca.</p>
     *
     * @param extraValues valores que deben quedar visibles, como el de un punto marcado
     */
    public static Range suggestYRange(Collection<Expression> functions, Range xRange, double... extraValues) {
        List<Double> values = new ArrayList<>();
        for (Expression function : functions) {
            for (Sample s : sample(function, xRange, SCALE_SAMPLES)) {
                if (!Double.isNaN(s.y())) {
                    values.add(s.y());
                }
            }
        }
        if (values.isEmpty()) {
            return new Range(-MIN_HEIGHT * 2.5, MIN_HEIGHT * 2.5);
        }
        double[] sorted = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double spread = Math.max(q3 - q1, MIN_HEIGHT) * 3;
        double low = Math.max(percentile(sorted, 0.02), q1 - spread);
        double high = Math.min(percentile(sorted, 0.98), q3 + spread);
        for (double value : extraValues) {
            if (Double.isFinite(value)) {
                low = Math.min(low, value);
                high = Math.max(high, value);
            }
        }

        // Incluir el eje x si queda cerca del rango
        double height = Math.max(high - low, MIN_HEIGHT);
        if (low > 0 && low < height) {
            low = 0;
        } else if (high < 0 && -high < height) {
            high = 0;
        }

        double margin = Math.max(high - low, MIN_HEIGHT) * 0.1;
        double center = (low + high) / 2;
        double span = Math.max(high - low, MIN_HEIGHT) + 2 * margin;
        return Range.centeredAt(center, span);
    }

    private static double percentile(double[] sorted, double fraction) {
        return sorted[(int) Math.min(sorted.length - 1, Math.floor(sorted.length * fraction))];
    }

    /**
     * Divide la curva en tramos continuos para dibujarla: se corta donde la
     * función no está definida y donde salta más que {@code maxJump} entre
     * dos puntos seguidos (una asíntota vertical, como en {@code tan(x)}).
     */
    public static List<List<Sample>> segments(List<Sample> samples, double maxJump) {
        List<List<Sample>> segments = new ArrayList<>();
        List<Sample> current = new ArrayList<>();
        Sample previous = null;
        for (Sample s : samples) {
            boolean defined = !Double.isNaN(s.y());
            boolean jump = previous != null && defined && Math.abs(s.y() - previous.y()) > maxJump;
            if (!defined || jump) {
                if (current.size() > 1) {
                    segments.add(current);
                }
                current = new ArrayList<>();
            }
            if (defined) {
                current.add(s);
            }
            previous = defined ? s : null;
        }
        if (current.size() > 1) {
            segments.add(current);
        }
        return segments;
    }
}
