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
        return enclosed(f, g, "x");
    }

    /**
     * Área de la región encerrada por f y g respecto de la variable indicada.
     * Las curvas deben depender solo de esa variable: si usan la otra, el área
     * dependería de un parámetro cuyo valor no se proporciona.
     */
    public CalculationResult enclosed(Expression f, Expression g, String variableName)
            throws ParameterException {
        validateVariable(f, g, variableName);
        Expression h = difference(f, g);
        List<Intersection> cuts = CurveIntersections.find(h, SEARCH_RANGE.min(), SEARCH_RANGE.max());
        String range = "[" + fmt(SEARCH_RANGE.min()) + ", " + fmt(SEARCH_RANGE.max()) + "]";
        if (cuts.size() < 2) {
            String where = cuts.isEmpty() ? "no se cortan"
                    : "solo se cortan en " + variableName + " = " + cuts.getFirst().value();
            throw new ParameterException("Entre " + variableName + " = " + fmt(SEARCH_RANGE.min()) + " y "
                    + variableName + " = " + fmt(SEARCH_RANGE.max()) + " las curvas " + where
                    + ", así que no encierran una región: escribe a y b.");
        }
        if (cuts.size() > MAX_AUTOMATIC_INTERSECTIONS) {
            throw new ParameterException("f y g se cortan " + cuts.size() + " veces en " + range
                    + ": escribe a y b para elegir la región.");
        }

        List<SolutionStep> steps = new ArrayList<>();
        addFunctions(f, g, steps, variableName);
        steps.add(new SolutionStep("Puntos de corte: f(" + variableName + ") = g(" + variableName
                + ") ⇔ f(" + variableName + ") - g(" + variableName + ") = " + h + " = 0"));
        for (Intersection cut : cuts) {
            steps.add(new SolutionStep(cutDescription(cut, f, variableName), 1));
        }
        Intersection first = cuts.getFirst();
        Intersection last = cuts.getLast();
        steps.add(new SolutionStep("La región encerrada va del primer al último corte: [" + first.value() + ", "
                + last.value() + "]"));
        return solve(f, g, h, cuts, steps, variableName);
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
        return between(f, g, a, b, "x");
    }

    /** Área entre f y g en [a, b], respecto de x o y. */
    public CalculationResult between(Expression f, Expression g, Expression a, Expression b,
                                     String variableName) throws ParameterException {
        validateVariable(f, g, variableName);
        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        if (!(av < bv)) {
            throw new ParameterException("a debe ser menor que b.");
        }
        Expression h = difference(f, g);
        List<SolutionStep> steps = new ArrayList<>();
        addFunctions(f, g, steps, variableName);

        List<Intersection> inside = CurveIntersections.find(h, av, bv).stream()
                .filter(cut -> !near(cut.x(), av) && !near(cut.x(), bv))
                .toList();
        if (inside.isEmpty()) {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): ninguno, f("
                    + variableName + ") - g(" + variableName + ") = " + h
                    + " no cambia de signo, así que una sola curva va arriba en todo el intervalo"));
        } else {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): f(" + variableName
                    + ") - g(" + variableName + ") = " + h + " = 0"));
            for (Intersection cut : inside) {
                steps.add(new SolutionStep(cutDescription(cut, f, variableName), 1));
            }
        }
        List<Intersection> limits = new ArrayList<>();
        limits.add(new Intersection(av, a, true));
        limits.addAll(inside);
        limits.add(new Intersection(bv, b, true));
        return solve(f, g, h, limits, steps, variableName);
    }

    // ------------------------------------------------------------ cálculo

    /**
     * Integra cada tramo entre dos cortes seguidos y suma las áreas.
     */
    private CalculationResult solve(Expression f, Expression g, Expression h, List<Intersection> limits,
                                    List<SolutionStep> steps, String variableName) {
        Intersection first = limits.getFirst();
        Intersection last = limits.getLast();
        String interval = "[" + first.value() + ", " + last.value() + "]";
        Graph emptyGraph = "x".equals(variableName) ? graph(f, g, limits, List.of()) : null;
        if ("y".equals(variableName)) {
            steps.add(new SolutionStep("Nota: el visor actual solo dibuja gráficas de la forma y = f(x); "
                    + "por eso no se muestra una gráfica para el cálculo respecto de y."));
        }

        Optional<Double> gap = firstDiscontinuity(h, first.x(), last.x());
        if (gap.isPresent()) {
            steps.add(new SolutionStep("f(" + variableName + ") - g(" + variableName + ") no está definida en "
                    + variableName + " ≈ " + fmt(gap.get()) + ", dentro de " + interval
                    + ": el área es una integral impropia. Prueba con un intervalo donde ambas curvas sean continuas."));
            return new CalculationResult("Área entre curvas: integral impropia", steps, emptyGraph);
        }

        Optional<Expression> antiderivative = antiderivative(h, steps, variableName);
        boolean allExact = antiderivative.isPresent() && limits.stream().allMatch(Intersection::exact);

        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i + 1 < limits.size(); i++) {
            segments.add(segment(f, g, h, antiderivative, limits.get(i), limits.get(i + 1),
                    i + 1, limits.size() > 2, steps, variableName));
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
            double net = segments.stream().mapToDouble(s -> s.fIsPositive() ? s.area() : -s.area()).sum();
            String explanation = "x".equals(variableName)
                    ? "los tramos donde g va arriba restan. Por eso se integra por tramos."
                    : "los tramos donde g queda a la derecha restan. Por eso se integra por tramos.";
            steps.add(new SolutionStep("Sin valor absoluto, ∫" + interval + " (f(" + variableName + ") - g("
                    + variableName + ")) d" + variableName + " ≈ " + fmt(net) + ": " + explanation, 1));
        }

        double reference = segments.stream().mapToDouble(s -> simpson(h, s.from().x(), s.to().x())).sum();
        if (antiderivative.isPresent() && Double.isFinite(reference)) {
            boolean agrees = Math.abs(reference - total) <= 1e-6 * Math.max(1, Math.abs(total));
            steps.add(new SolutionStep("Comprobación numérica (Simpson con n = " + SIMPSON_SUBINTERVALS
                    + " por tramo): ≈ " + fmt(reference) + (agrees ? " ✓" : " ✗ (no coincide)")));
        }
        Graph resultGraph = "x".equals(variableName) ? graph(f, g, limits, segments) : null;
        return new CalculationResult(summary, steps, resultGraph);
    }

    /**
     * Un tramo entre dos cortes seguidos.
     *
     * @param exact  área exacta, o {@code null} si se aproximó
     * @param area   área (siempre positiva)
     * @param fIsPositive si f − g es no negativa en el tramo (f va arriba para x, a la derecha para y)
     */
    private record Segment(int index, Intersection from, Intersection to, Expression exact, double area,
                           boolean fIsPositive) {
        String name() {
            return "A" + FormatUtils.subscript(index);
        }
    }

    private Segment segment(Expression f, Expression g, Expression h, Optional<Expression> antiderivative,
                            Intersection from, Intersection to, int index, boolean numbered,
                            List<SolutionStep> steps, String variableName) {
        double middle = (from.x() + to.x()) / 2;
        boolean fIsPositive = h.evaluate(middle) >= 0;
        String interval = "[" + from.value() + ", " + to.value() + "]";
        String positiveCurve = fIsPositive ? "f" : "g";
        String negativeCurve = fIsPositive ? "g" : "f";
        String title = "x".equals(variableName)
                ? (numbered ? "Tramo " + index + ": " : "Tramo ") + interval + ": en x = " + fmtShort(middle)
                + ", f = " + fmtShort(f.evaluate(middle)) + " y g = " + fmtShort(g.evaluate(middle))
                + ", así que " + positiveCurve + " va arriba"
                : (numbered ? "Tramo " + index + ": " : "Tramo ") + interval + ": en y = " + fmtShort(middle)
                + ", f(y) = " + fmtShort(f.evaluate(middle)) + " y g(y) = " + fmtShort(g.evaluate(middle))
                + ", así que " + positiveCurve + " va a la derecha";
        steps.add(new SolutionStep(title));

        Expression integrand = fIsPositive ? h : Simplifier.simplify(Simplifier.negate(h));
        String name = numbered ? "A" + FormatUtils.subscript(index) : "A";
        String integral = "x".equals(variableName)
                ? name + " = ∫" + interval + " (" + positiveCurve + "(x) - " + negativeCurve + "(x)) dx = ∫"
                + interval + " (" + integrand + ") dx"
                : name + " = ∫" + interval + " (" + positiveCurve + "(y) - " + negativeCurve + "(y)) dy = ∫"
                + interval + " (" + integrand + ") dy";

        if (antiderivative.isEmpty()) {
            double area = simpson(integrand, from.x(), to.x());
            steps.add(new SolutionStep(integral + " ≈ " + fmt(area) + " (Simpson, n = " + SIMPSON_SUBINTERVALS + ")",
                    1));
            return new Segment(index, from, to, null, area, fIsPositive);
        }

        // Si g va arriba, ∫(g - f) = -(H(b) - H(a)) = H(a) - H(b)
        Expression atTo = evaluateAt(antiderivative.get(), to.value(), variableName);
        Expression atFrom = evaluateAt(antiderivative.get(), from.value(), variableName);
        Expression exact = tidy(fIsPositive ? new Difference(atTo, atFrom) : new Difference(atFrom, atTo));
        String tfc = fIsPositive
                ? "H(" + to.value() + ") - H(" + from.value() + ")"
                : "H(" + from.value() + ") - H(" + to.value() + ")";
        steps.add(new SolutionStep(integral, 1));
        steps.add(new SolutionStep("= " + tfc + " = " + withApproximation(exact), 2));
        double area = exact.evaluate(0);
        boolean exactLimits = from.exact() && to.exact();
        return new Segment(index, from, to, exactLimits ? exact : null, Math.abs(area), fIsPositive);
    }

    /**
     * Antiderivada H(x) de f − g, con sus pasos; vacía si no tiene una
     * elemental (entonces cada tramo se aproxima).
     */
    private Optional<Expression> antiderivative(Expression h, List<SolutionStep> steps, String variableName) {
        steps.add(new SolutionStep("Antiderivada de f(" + variableName + ") - g(" + variableName + "):"));
        List<SolutionStep> integration = new ArrayList<>();
        try {
            Expression result = indefinite.antiderivative(h, integration, 1, variableName);
            steps.addAll(integration);
            steps.add(new SolutionStep("H(" + variableName + ") = " + result, 1));
            return Optional.of(result);
        } catch (UnsupportedIntegralException e) {
            steps.add(new SolutionStep(e.getMessage() + " Cada tramo se aproxima con la regla de Simpson.", 1));
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------ apoyo

    private static void addFunctions(Expression f, Expression g, List<SolutionStep> steps, String variableName) {
        boolean zero = g instanceof Constant c && c.isNumber(0);
        if ("x".equals(variableName)) {
            steps.add(new SolutionStep("Curvas: f(x) = " + f + " y g(x) = " + g + (zero ? " (el eje x)" : "")));
        } else {
            steps.add(new SolutionStep("Curvas: f(y) = " + f + " y g(y) = " + g + (zero ? " (función cero)" : "")));
        }
        String sides = "x".equals(variableName)
                ? "(la de arriba) - (la de abajo)"
                : "(la de la derecha) - (la de la izquierda)";
        steps.add(new SolutionStep("Área entre curvas: A = ∫[a, b] |f(" + variableName + ") - g("
                + variableName + ")| d" + variableName + ", integrando en cada tramo " + sides));
    }

    /** {@code x = 1  →  (1, 1)}. */
    private static String cutDescription(Intersection cut, Expression f, String variableName) {
        String exact = cut.exact() ? "" : " (aproximado)";
        String approx = cut.exact() && !(cut.value() instanceof Constant c && c.isNumber())
                ? " ≈ " + fmtShort(cut.x()) : "";
        String point = "x".equals(variableName)
                ? pointLabel(cut.x(), f.evaluate(cut.x()))
                : pointLabel(f.evaluate(cut.x()), cut.x());
        return variableName + " = " + cut.value() + approx + exact + "  →  " + point;
    }

    /** Verifica que las curvas sean funciones de una sola variable de integración. */
    private static void validateVariable(Expression f, Expression g, String variableName)
            throws ParameterException {
        if (!"x".equals(variableName) && !"y".equals(variableName)) {
            throw new ParameterException("La variable de integración debe ser x o y.");
        }
        String otherVariable = "x".equals(variableName) ? "y" : "x";
        Variable other = new Variable(otherVariable);
        if (f.contains(other) || g.contains(other)) {
            throw new ParameterException("Para calcular el área respecto de " + variableName
                    + ", f y g deben depender solo de " + variableName + ". La otra variable ("
                    + otherVariable + ") tendría que tener un valor fijo para obtener un área numérica.");
        }
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

    private static Expression evaluateAt(Expression antiderivative, Expression point, String variableName) {
        return tidy(antiderivative.replace(new Variable(variableName), point));
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
                .map(s -> s.fIsPositive()
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
