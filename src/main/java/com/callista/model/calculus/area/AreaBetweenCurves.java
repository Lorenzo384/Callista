package com.callista.model.calculus.area;

import com.callista.model.calculus.area.CurveIntersections.Intersection;
import com.callista.model.calculus.integral.IndefiniteIntegral;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.GraphPoint;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.ParameterException;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;
import com.callista.model.graph.Range;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Área entre dos curvas {@code A = ∫[a, b] |f(x) - g(x)| dx}.
 *
 * <ol>
 *     <li>Se buscan los <b>puntos de corte</b> (las raíces de f − g). Si no
 *     se dan a y b, la región va del primer al último corte.</li>
 *     <li>Los cortes parten [a, b] en <b>tramos</b>; en cada uno, una de las
 *     curvas va arriba (se ve evaluando en el punto medio).</li>
 *     <li>En cada tramo se integra (arriba − abajo) con el Teorema
 *     Fundamental, usando una sola antiderivada de f − g, y se suman las
 *     áreas. Si no hay antiderivada elemental, se aproxima con Simpson.</li>
 * </ol>
 */
public class AreaBetweenCurves {

    /** Intervalo donde se buscan los cortes cuando no se dan a y b. */
    public static final Range SEARCH_RANGE = new Range(-10, 10);
    /** Con más cortes que estos (como sen x y cos x), se piden a y b. */
    private static final int MAX_AUTOMATIC_INTERSECTIONS = 4;
    /** Subintervalos de Simpson para comprobar o, sin antiderivada, aproximar cada tramo. */
    private static final int SIMPSON_SUBINTERVALS = 1000;
    /** Puntos donde se revisa que f − g sea continua. */
    private static final int CONTINUITY_SAMPLES = 2001;

    private static final Variable X = new Variable("x");
    private static final Constant ZERO = new Constant(0);

    private final IndefiniteIntegral indefinite = new IndefiniteIntegral();

    /**
     * Área de la región encerrada por f y g: de su primer a su último punto de
     * corte (buscados en {@link #SEARCH_RANGE}).
     *
     * @throws ParameterException si las curvas no encierran una región
     *                            (se cortan menos de dos veces o demasiadas)
     */
    public CalculationResult enclosed(Expression f, Expression g) throws ParameterException {
        Expression h = difference(f, g);
        List<Intersection> cuts = CurveIntersections.find(h, SEARCH_RANGE.min(), SEARCH_RANGE.max());
        String range = "[" + fmt(SEARCH_RANGE.min()) + ", " + fmt(SEARCH_RANGE.max()) + "]";
        if (cuts.size() < 2) {
            String where = cuts.isEmpty() ? "no se cortan" : "solo se cortan en x = " + cuts.getFirst().value();
            throw new ParameterException("Entre x = " + fmt(SEARCH_RANGE.min()) + " y x = " + fmt(SEARCH_RANGE.max())
                    + " las curvas " + where + ", así que no encierran una región: escribe a y b.");
        }
        if (cuts.size() > MAX_AUTOMATIC_INTERSECTIONS) {
            throw new ParameterException("f y g se cortan " + cuts.size() + " veces en " + range
                    + ": escribe a y b para elegir la región.");
        }

        List<SolutionStep> steps = new ArrayList<>();
        addFunctions(f, g, steps);
        steps.add(new SolutionStep("Puntos de corte: f(x) = g(x) ⇔ f(x) - g(x) = " + h + " = 0"));
        for (Intersection cut : cuts) {
            steps.add(new SolutionStep(cutDescription(cut, f), 1));
        }
        Intersection first = cuts.getFirst();
        Intersection last = cuts.getLast();
        steps.add(new SolutionStep("La región encerrada va del primer al último corte: [" + first.value() + ", "
                + last.value() + "]"));
        return solve(f, g, h, cuts, steps);
    }

    /**
     * Área entre f y g en [a, b]: los cortes que haya adentro parten el
     * intervalo en tramos.
     *
     * @param a límite inferior, sin evaluar, para conservarlo exacto
     * @param b límite superior, sin evaluar
     * @throws ParameterException si {@code a} no es menor que {@code b}
     */
    public CalculationResult between(Expression f, Expression g, Expression a, Expression b)
            throws ParameterException {
        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        if (!(av < bv)) {
            throw new ParameterException("a debe ser menor que b.");
        }
        Expression h = difference(f, g);
        List<SolutionStep> steps = new ArrayList<>();
        addFunctions(f, g, steps);

        List<Intersection> inside = CurveIntersections.find(h, av, bv).stream()
                .filter(cut -> !near(cut.x(), av) && !near(cut.x(), bv))
                .toList();
        if (inside.isEmpty()) {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): ninguno, f(x) - g(x) = "
                    + h + " no cambia de signo, así que una sola curva va arriba en todo el intervalo"));
        } else {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): f(x) - g(x) = " + h
                    + " = 0"));
            for (Intersection cut : inside) {
                steps.add(new SolutionStep(cutDescription(cut, f), 1));
            }
        }
        List<Intersection> limits = new ArrayList<>();
        limits.add(new Intersection(av, a, true));
        limits.addAll(inside);
        limits.add(new Intersection(bv, b, true));
        return solve(f, g, h, limits, steps);
    }

    // ------------------------------------------------------------ cálculo

    /**
     * Integra cada tramo entre dos cortes seguidos y suma las áreas.
     */
    private CalculationResult solve(Expression f, Expression g, Expression h, List<Intersection> limits,
                                    List<SolutionStep> steps) {
        Intersection first = limits.getFirst();
        Intersection last = limits.getLast();
        String interval = "[" + first.value() + ", " + last.value() + "]";
        Graph emptyGraph = graph(f, g, limits, List.of());

        Optional<Double> gap = firstDiscontinuity(h, first.x(), last.x());
        if (gap.isPresent()) {
            steps.add(new SolutionStep("f(x) - g(x) no está definida en x ≈ " + fmt(gap.get()) + ", dentro de "
                    + interval + ": el área es una integral impropia. Prueba con un intervalo donde ambas "
                    + "curvas sean continuas."));
            return new CalculationResult("Área entre curvas: integral impropia", steps, emptyGraph);
        }

        Optional<Expression> antiderivative = antiderivative(h, steps);
        boolean allExact = antiderivative.isPresent() && limits.stream().allMatch(Intersection::exact);

        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i + 1 < limits.size(); i++) {
            segments.add(segment(f, g, h, antiderivative, limits.get(i), limits.get(i + 1),
                    i + 1, limits.size() > 2, steps));
        }

        double total = segments.stream().mapToDouble(Segment::area).sum();
        String summary;
        if (allExact) {
            Expression exact = tidy(sumOf(segments.stream().map(Segment::exact).toList()));
            String names = segments.size() > 1 ? joinNames(segments) + " = " : "";
            steps.add(new SolutionStep("Área total: A = " + names + withApproximation(exact)));
            summary = "Área = " + withApproximation(exact);
        } else {
            String names = segments.size() > 1 ? joinNames(segments) + " ≈ " : "≈ ";
            steps.add(new SolutionStep("Área total: A " + names + fmt(total)));
            summary = "Área ≈ " + fmt(total);
        }
        if (segments.size() > 1) {
            double net = segments.stream().mapToDouble(s -> s.fOnTop() ? s.area() : -s.area()).sum();
            steps.add(new SolutionStep("Sin valor absoluto, ∫" + interval + " (f(x) - g(x)) dx ≈ " + fmt(net)
                    + ": los tramos donde g va arriba restan. Por eso se integra por tramos.", 1));
        }

        double reference = segments.stream().mapToDouble(s -> simpson(h, s.from().x(), s.to().x())).sum();
        if (antiderivative.isPresent() && Double.isFinite(reference)) {
            boolean agrees = Math.abs(reference - total) <= 1e-6 * Math.max(1, Math.abs(total));
            steps.add(new SolutionStep("Comprobación numérica (Simpson con n = " + SIMPSON_SUBINTERVALS
                    + " por tramo): ≈ " + fmt(reference) + (agrees ? " ✓" : " ✗ (no coincide)")));
        }
        return new CalculationResult(summary, steps, graph(f, g, limits, segments));
    }

    /**
     * Un tramo entre dos cortes seguidos.
     *
     * @param exact  área exacta, o {@code null} si se aproximó
     * @param area   área (siempre positiva)
     * @param fOnTop si f va arriba en el tramo
     */
    private record Segment(int index, Intersection from, Intersection to, Expression exact, double area,
                           boolean fOnTop) {
        String name() {
            return "A" + FormatUtils.subscript(index);
        }
    }

    private Segment segment(Expression f, Expression g, Expression h, Optional<Expression> antiderivative,
                            Intersection from, Intersection to, int index, boolean numbered,
                            List<SolutionStep> steps) {
        double middle = (from.x() + to.x()) / 2;
        boolean fOnTop = h.evaluate(middle) >= 0;
        String interval = "[" + from.value() + ", " + to.value() + "]";
        String top = fOnTop ? "f" : "g";
        String bottom = fOnTop ? "g" : "f";
        String title = (numbered ? "Tramo " + index + ": " : "Tramo ") + interval + ": en x = " + fmtShort(middle)
                + ", f = " + fmtShort(f.evaluate(middle)) + " y g = " + fmtShort(g.evaluate(middle))
                + ", así que " + top + " va arriba";
        steps.add(new SolutionStep(title));

        Expression integrand = fOnTop ? h : Simplifier.simplify(Simplifier.negate(h));
        String name = numbered ? "A" + FormatUtils.subscript(index) : "A";
        String integral = name + " = ∫" + interval + " (" + top + "(x) - " + bottom + "(x)) dx = ∫" + interval
                + " (" + integrand + ") dx";

        if (antiderivative.isEmpty()) {
            double area = simpson(integrand, from.x(), to.x());
            steps.add(new SolutionStep(integral + " ≈ " + fmt(area) + " (Simpson, n = " + SIMPSON_SUBINTERVALS + ")",
                    1));
            return new Segment(index, from, to, null, area, fOnTop);
        }

        // Si g va arriba, ∫(g - f) = -(H(b) - H(a)) = H(a) - H(b)
        Expression atTo = evaluateAt(antiderivative.get(), to.value());
        Expression atFrom = evaluateAt(antiderivative.get(), from.value());
        Expression exact = tidy(fOnTop ? new Difference(atTo, atFrom) : new Difference(atFrom, atTo));
        String tfc = fOnTop
                ? "H(" + to.value() + ") - H(" + from.value() + ")"
                : "H(" + from.value() + ") - H(" + to.value() + ")";
        steps.add(new SolutionStep(integral, 1));
        steps.add(new SolutionStep("= " + tfc + " = " + withApproximation(exact), 2));
        double area = exact.evaluate(0);
        boolean exactLimits = from.exact() && to.exact();
        return new Segment(index, from, to, exactLimits ? exact : null, Math.abs(area), fOnTop);
    }

    /**
     * Antiderivada H(x) de f − g, con sus pasos; vacía si no tiene una
     * elemental (entonces cada tramo se aproxima).
     */
    private Optional<Expression> antiderivative(Expression h, List<SolutionStep> steps) {
        steps.add(new SolutionStep("Antiderivada de f(x) - g(x):"));
        List<SolutionStep> integration = new ArrayList<>();
        try {
            Expression result = indefinite.antiderivative(h, integration, 1);
            steps.addAll(integration);
            steps.add(new SolutionStep("H(x) = " + result, 1));
            return Optional.of(result);
        } catch (UnsupportedIntegralException e) {
            steps.add(new SolutionStep(e.getMessage() + " Cada tramo se aproxima con la regla de Simpson.", 1));
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------ apoyo

    private static void addFunctions(Expression f, Expression g, List<SolutionStep> steps) {
        boolean axis = g instanceof Constant c && c.isNumber(0);
        steps.add(new SolutionStep("Curvas: f(x) = " + f + " y g(x) = " + g + (axis ? " (el eje x)" : "")));
        steps.add(new SolutionStep("Área entre curvas: A = ∫[a, b] |f(x) - g(x)| dx, integrando en cada tramo "
                + "(la de arriba) - (la de abajo)"));
    }

    /** {@code x = 1  →  (1, 1)}. */
    private static String cutDescription(Intersection cut, Expression f) {
        String exact = cut.exact() ? "" : " (aproximado)";
        String approx = cut.exact() && !(cut.value() instanceof Constant c && c.isNumber())
                ? " ≈ " + fmtShort(cut.x()) : "";
        return "x = " + cut.value() + approx + exact + "  →  " + pointLabel(cut.x(), f.evaluate(cut.x()));
    }

    /**
     * {@code f - g} simplificada.
     *
     * @throws ParameterException si f y g son la misma función
     */
    private static Expression difference(Expression f, Expression g) throws ParameterException {
        Expression h = Simplifier.simplify(new Difference(f, g));
        if (h instanceof Constant c && c.isNumber(0)) {
            throw new ParameterException("f y g son la misma función: no hay región entre ellas.");
        }
        return h;
    }

    private static Expression evaluateAt(Expression antiderivative, Expression point) {
        return tidy(antiderivative.replace(X, point));
    }

    /**
     * La forma más corta entre la simplificada y la desarrollada: con cortes
     * como (1 + √5)/2, desarrollar las potencias agrupa los radicales.
     */
    private static Expression tidy(Expression e) {
        Expression simplified = Simplifier.simplify(e);
        Expression expanded = Simplifier.expand(e);
        return expanded.toString().length() < simplified.toString().length() ? expanded : simplified;
    }

    private static Expression sumOf(List<Expression> terms) {
        Expression total = terms.getFirst();
        for (Expression term : terms.subList(1, terms.size())) {
            total = new Sum(total, term);
        }
        return total;
    }

    /** {@code A₁ + A₂ + A₃}. */
    private static String joinNames(List<Segment> segments) {
        return String.join(" + ", segments.stream().map(Segment::name).toList());
    }

    private static double simpson(Expression function, double a, double b) {
        try {
            return Math.abs(NumericalMethod.SIMPSON.getRule().integrate(function, a, b, SIMPSON_SUBINTERVALS)
                    .value());
        } catch (ParameterException impossible) {
            return Double.NaN;
        }
    }

    private static Optional<Double> firstDiscontinuity(Expression h, double from, double to) {
        for (int i = 0; i < CONTINUITY_SAMPLES; i++) {
            double x = from + (to - from) * i / (CONTINUITY_SAMPLES - 1);
            double y = h.evaluate(x);
            if (!Double.isFinite(y) || Math.abs(y) > 1e12) {
                return Optional.of(x);
            }
        }
        return Optional.empty();
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9 * (1 + Math.abs(b));
    }

    /**
     * Las dos curvas, los puntos de corte y la región sombreada de cada
     * tramo, con la curva de arriba como borde superior.
     */
    private static Graph graph(Expression f, Expression g, List<Intersection> limits, List<Segment> segments) {
        List<GraphCurve> curves = List.of(
                new GraphCurve("f(x) = " + f, f, CurveStyle.FUNCTION),
                new GraphCurve("g(x) = " + g, g, CurveStyle.SECOND_FUNCTION));
        List<GraphPoint> points = new ArrayList<>();
        for (Intersection limit : limits) {
            double y = f.evaluate(limit.x());
            if (Double.isFinite(y) && Math.abs(y - g.evaluate(limit.x())) < 1e-7) {
                points.add(new GraphPoint(pointLabel(limit.x(), y), limit.x(), y));
            }
        }
        String label = "Área entre f y g";
        List<GraphArea> areas = segments.stream()
                .map(s -> s.fOnTop()
                        ? new GraphArea(label, f, g, s.from().x(), s.to().x())
                        : new GraphArea(label, g, f, s.from().x(), s.to().x()))
                .toList();
        double a = limits.getFirst().x();
        double b = limits.getLast().x();
        Range xRange = Range.centeredAt((a + b) / 2, Math.max(Math.abs(b - a) * 1.6, 2));
        return new Graph(curves, points, xRange, areas, List.of());
    }

    /** {@code 1/6 ≈ 0.1666666667}, o solo el número si ya es un número. */
    private static String withApproximation(Expression e) {
        boolean plainNumber = e instanceof Constant c && c.isNumber();
        return plainNumber ? e.toString() : e + " ≈ " + fmt(e.evaluate(0));
    }

    private static String pointLabel(double x, double y) {
        return "(" + fmtShort(x) + ", " + fmtShort(y) + ")";
    }

    private static String fmt(double value) {
        return FormatUtils.formatNumber(value);
    }

    private static String fmtShort(double value) {
        return FormatUtils.formatShort(value);
    }

    /** Constante 0, para cuando g se deja vacía (el área entre f y el eje x). */
    public static Expression xAxis() {
        return ZERO;
    }
}
