package com.callista.model.calculus.area;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.expression.Simplifier;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

/**
 * Puntos donde se cortan dos curvas: las raíces de {@code h(x) = f(x) - g(x)}
 * en un intervalo.
 *
 * <p>Se evalúa h en muchos puntos y se busca:</p>
 * <ul>
 *     <li>un <b>cambio de signo</b> entre dos puntos seguidos, que se refina
 *     con bisección (si h no se acerca a 0, era una asíntota y se descarta);</li>
 *     <li>un <b>mínimo de |h|</b> sin cambio de signo, donde las curvas se
 *     tocan sin cruzarse (por ejemplo, x² y 0 en x = 0).</li>
 * </ul>
 *
 * <p>Cada raíz se reconoce, si se puede, como un valor exacto sencillo
 * ({@code 1/2}, {@code π/4}, {@code (1 + √5)/2}) para que la integral se
 * calcule de forma exacta con el Teorema Fundamental.</p>
 */
public final class CurveIntersections {

    /** Puntos donde se evalúa h para buscar las raíces. */
    private static final int SAMPLES = 4000;
    /** Iteraciones de bisección y de búsqueda del mínimo. */
    private static final int ITERATIONS = 200;
    /** Denominadores que se prueban al reconocer un valor exacto. */
    private static final int MAX_DENOMINATOR = 12;
    /** Raíces cuadradas √n que se prueban, con n de 2 a este valor. */
    private static final int MAX_RADICAND = 30;
    /** Denominadores que se prueban junto con una raíz cuadrada: (p ± √n)/q. */
    private static final int MAX_RADICAL_DENOMINATOR = 6;

    private CurveIntersections() {
    }

    /**
     * Un punto de corte.
     *
     * @param x     abscisa, para los cálculos numéricos
     * @param value la misma abscisa como expresión: exacta si se reconoció
     *              ({@code π/4}) o redondeada a 10 cifras si no
     * @param exact si {@code value} es exacto
     */
    public record Intersection(double x, Expression value, boolean exact) {
    }

    /**
     * Raíces de h en [from, to], ordenadas de izquierda a derecha.
     */
    public static List<Intersection> find(Expression h, double from, double to) {
        List<Double> roots = new ArrayList<>();
        double[] xs = new double[SAMPLES + 1];
        double[] ys = new double[SAMPLES + 1];
        for (int i = 0; i <= SAMPLES; i++) {
            xs[i] = from + (to - from) * i / SAMPLES;
            ys[i] = h.evaluate(xs[i]);
        }
        for (int i = 0; i <= SAMPLES; i++) {
            if (ys[i] == 0) {
                roots.add(xs[i]);
            } else if (i < SAMPLES && Double.isFinite(ys[i]) && Double.isFinite(ys[i + 1]) && ys[i + 1] != 0
                    && Math.signum(ys[i]) != Math.signum(ys[i + 1])) {
                double root = bisect(h::evaluate, xs[i], xs[i + 1]);
                if (isRoot(h, root)) {
                    roots.add(root);
                }
            } else if (i > 0 && i < SAMPLES && isLocalMinimumOfAbs(ys[i - 1], ys[i], ys[i + 1])) {
                double root = minimizeAbs(h::evaluate, xs[i - 1], xs[i + 1]);
                if (Math.abs(h.evaluate(root)) < 1e-10) {
                    roots.add(root);
                }
            }
        }

        List<Intersection> result = new ArrayList<>();
        roots.stream().sorted().forEach(root -> {
            boolean repeated = !result.isEmpty()
                    && Math.abs(result.getLast().x() - root) < 1e-6 * (1 + Math.abs(root));
            if (!repeated) {
                result.add(recognize(h, root));
            }
        });
        result.sort(Comparator.comparingDouble(Intersection::x));
        return result;
    }

    /**
     * La raíz como valor exacto si es uno sencillo; si no, redondeada.
     */
    static Intersection recognize(Expression h, double root) {
        Optional<Expression> exact = exactValue(root);
        if (exact.isPresent()) {
            double x = exact.get().evaluate(0);
            if (Math.abs(h.evaluate(x)) <= Math.abs(h.evaluate(root)) * 10 + 1e-12) {
                return new Intersection(x, exact.get(), true);
            }
        }
        double rounded = Double.parseDouble(FormatUtils.formatNumber(root));
        return new Intersection(root, new Constant(rounded), false);
    }

    /**
     * Prueba, en orden de sencillez: p/q, (p/q)·π y (p ± √n)/q.
     */
    static Optional<Expression> exactValue(double r) {
        double tolerance = 1e-8 * (1 + Math.abs(r));
        for (int q = 1; q <= MAX_DENOMINATOR; q++) {
            long p = Math.round(r * q);
            if (Math.abs((double) p / q - r) < tolerance) {
                return Optional.of(fraction(p, q));
            }
        }
        for (int q = 1; q <= MAX_DENOMINATOR; q++) {
            long p = Math.round(r * q / Math.PI);
            if (p != 0 && Math.abs(p * Math.PI / q - r) < tolerance) {
                return Optional.of(Simplifier.simplify(Simplifier.product(fraction(p, q), Constant.PI)));
            }
        }
        for (int n = 2; n <= MAX_RADICAND; n++) {
            double root = Math.sqrt(n);
            if (root == Math.rint(root)) {
                continue;
            }
            for (int q = 1; q <= MAX_RADICAL_DENOMINATOR; q++) {
                for (int sign = 1; sign >= -1; sign -= 2) {
                    long p = Math.round(r * q - sign * root);
                    if (Math.abs((p + sign * root) / q - r) < tolerance) {
                        return Optional.of(radical(p, sign, n, q));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** {@code p/q} simplificada. */
    private static Expression fraction(long p, long q) {
        return Simplifier.simplify(Simplifier.quotient(new Constant(p), new Constant(q)));
    }

    /** {@code (p ± √n)/q}, o {@code ±√n/q} si p = 0. */
    private static Expression radical(long p, int sign, int n, int q) {
        Expression sqrt = new FunctionCall(MathFunction.SQRT, new Constant(n));
        if (p == 0) {
            return Simplifier.simplify(Simplifier.product(fraction(sign, q), sqrt));
        }
        Expression numerator = sign > 0
                ? Simplifier.sum(new Constant(p), sqrt)
                : Simplifier.difference(new Constant(p), sqrt);
        return Simplifier.quotient(numerator, new Constant(q));
    }

    /** Bisección en [a, b], donde h cambia de signo. */
    private static double bisect(DoubleUnaryOperator h, double a, double b) {
        double fa = h.applyAsDouble(a);
        for (int i = 0; i < ITERATIONS && b - a > 0; i++) {
            double m = (a + b) / 2;
            if (m <= a || m >= b) {
                break;
            }
            double fm = h.applyAsDouble(m);
            if (fm == 0) {
                return m;
            }
            if (Math.signum(fm) == Math.signum(fa)) {
                a = m;
                fa = fm;
            } else {
                b = m;
            }
        }
        return (a + b) / 2;
    }

    /**
     * Un cambio de signo es un corte solo si h se acerca a 0 ahí; en una
     * asíntota (como 1/x en x = 0) también cambia de signo, pero es enorme.
     */
    private static boolean isRoot(Expression h, double x) {
        double value = h.evaluate(x);
        return Double.isFinite(value) && Math.abs(value) < 1e-7;
    }

    private static boolean isLocalMinimumOfAbs(double left, double middle, double right) {
        return Double.isFinite(left) && Double.isFinite(middle) && Double.isFinite(right)
                && Math.signum(left) == Math.signum(middle) && Math.signum(middle) == Math.signum(right)
                && Math.abs(middle) < Math.abs(left) && Math.abs(middle) <= Math.abs(right);
    }

    /** Búsqueda de la sección dorada del mínimo de |h| en [a, b]. */
    private static double minimizeAbs(DoubleUnaryOperator h, double a, double b) {
        double ratio = (Math.sqrt(5) - 1) / 2;
        double c = b - ratio * (b - a);
        double d = a + ratio * (b - a);
        for (int i = 0; i < ITERATIONS && b - a > 1e-15 * (1 + Math.abs(a)); i++) {
            if (Math.abs(h.applyAsDouble(c)) < Math.abs(h.applyAsDouble(d))) {
                b = d;
            } else {
                a = c;
            }
            c = b - ratio * (b - a);
            d = a + ratio * (b - a);
        }
        return (a + b) / 2;
    }
}
