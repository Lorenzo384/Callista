package com.callista.model.calculus.integral;

import com.callista.model.calculus.area.CurveIntersections;
import com.callista.model.calculus.area.CurveIntersections.Intersection;
import com.callista.model.calculus.limit.Limit;
import com.callista.model.calculus.limit.LimitCalculator;
import com.callista.model.calculus.limit.LimitPoint;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.ParameterException;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;
import com.callista.model.graph.Range;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Integral impropia: con un límite infinito (tipo 1) o con una asíntota
 * vertical de f en el intervalo (tipo 2).
 *
 * <p>Se define como un límite. Por ejemplo,
 * {@code ∫[1, ∞) f(x) dx = lim t→∞ [F(t) - F(1)]}. Si el límite es un número,
 * la integral <b>converge</b>; si es ±∞ o no existe, <b>diverge</b>.</p>
 *
 * <p>Si hay varios puntos problemáticos (los dos extremos, o una asíntota
 * adentro), la integral se parte en tramos con un solo punto problemático
 * cada uno: converge solo si <b>todos</b> los tramos convergen. Sin
 * antiderivada elemental, cada tramo se estima numéricamente.</p>
 */
public class ImproperIntegral {

    /** Si un límite es infinito, se buscan asíntotas hasta esta distancia del otro límite. */
    private static final double SEARCH_WIDTH = 50;
    /** Subintervalos de Simpson en cada tramo de la estimación numérica. */
    private static final int SIMPSON_SUBINTERVALS = 2000;
    /** Pasos de la estimación numérica: hasta 10^7 o hasta 10^-8 del punto. */
    private static final int NUMERIC_STEPS = 8;

    private static final Variable T = new Variable("t");

    private final IndefiniteIntegral indefinite = new IndefiniteIntegral();
    private final LimitCalculator limits = new LimitCalculator();

    /**
     * Indica si la integral es impropia: algún límite es infinito o f tiene
     * una asíntota vertical en [a, b].
     */
    public static boolean isImproper(Expression f, double a, double b) {
        return Double.isInfinite(a) || Double.isInfinite(b)
                || !asymptotes(f, Math.min(a, b), Math.max(a, b)).isEmpty();
    }

    /**
     * Puntos de [a, b] donde f no está acotada (asíntotas verticales): son
     * las raíces de 1/f donde |f| crece al acercarse.
     */
    static List<Intersection> asymptotes(Expression f, double a, double b) {
        double from = Double.isFinite(a) ? a : (Double.isFinite(b) ? b : 0) - SEARCH_WIDTH;
        double to = Double.isFinite(b) ? b : (Double.isFinite(a) ? a : 0) + SEARCH_WIDTH;
        Expression reciprocal = Simplifier.simplify(new Quotient(new Constant(1), f));
        List<Intersection> found = new ArrayList<>(CurveIntersections.find(reciprocal, from, to).stream()
                .filter(point -> blowsUp(f, point.x(), from, to))
                .toList());
        // Un extremo donde f es enorme (tan(π/2) da 1.6e16 y no ∞)
        for (double end : new double[]{a, b}) {
            boolean listed = found.stream().anyMatch(p -> near(p.x(), end));
            double value = f.evaluate(end);
            boolean huge = !Double.isFinite(value) || Math.abs(value) > 1e12;
            if (Double.isFinite(end) && !listed && huge && blowsUp(f, end, from, to)) {
                found.add(CurveIntersections.recognize(reciprocal, end));
            }
        }
        found.sort(java.util.Comparator.comparingDouble(Intersection::x));
        return found;
    }

    /** |f| crece al acercarse a c por algún lado que esté dentro de [from, to]. */
    private static boolean blowsUp(Expression f, double c, double from, double to) {
        for (int side = -1; side <= 1; side += 2) {
            double far = c + side * 1e-4;
            if (far < from || far > to) {
                continue;
            }
            double v4 = Math.abs(f.evaluate(far));
            double v6 = Math.abs(f.evaluate(c + side * 1e-6));
            double v8 = Math.abs(f.evaluate(c + side * 1e-8));
            if (Double.isFinite(v4) && Double.isFinite(v6) && Double.isFinite(v8) && v8 > v6 && v6 > v4
                    && v8 > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resuelve la integral impropia {@code ∫[a, b] f(x) dx}.
     *
     * @param a límite inferior: un número exacto o -∞
     * @param b límite superior: un número exacto o ∞
     */
    public CalculationResult solve(Expression f, Expression a, Expression b) {
        return solve(f, a, b, "x");
    }

    /**
     * Resuelve la integral impropia respecto de x o y. Si f también depende
     * de la otra variable, esta queda como parámetro: la convergencia solo se
     * decide cuando las reglas no dependen de su valor.
     *
     * @param a            límite inferior: un número exacto o -∞
     * @param b            límite superior: un número exacto o ∞
     * @param variableName variable de integración: x o y
     */
    public CalculationResult solve(Expression f, Expression a, Expression b, String variableName) {
        if (!"x".equals(variableName) && !"y".equals(variableName)) {
            throw new IllegalArgumentException("La variable de integración debe ser x o y.");
        }
        Variable variable = new Variable(variableName);
        String other = "x".equals(variableName) ? "y" : "x";
        boolean parameterized = f.dependsOn(other);
        Names names = new Names(variableName, parameterized ? "(x,y)" : "(" + variableName + ")");
        // La gráfica es de y = f(x): solo se dibuja si f es función de x sola
        boolean graphable = "x".equals(variableName) && !parameterized;

        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        String integral = "∫" + interval(a, b) + " " + names.integrand();
        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep("Función: " + names.f() + " = " + f));
        if (!(av < bv)) {
            steps.add(new SolutionStep("Con límites infinitos o asíntotas, escribe a < b."));
            return new CalculationResult(integral + ": revisa los límites", steps);
        }

        // Con un parámetro, las asíntotas deben ser las mismas con cualquier valor de él
        List<Sample> samples = parameterized ? samples(f, new Variable(other), av, bv) : List.of();
        Expression inspected = samples.isEmpty() ? f : samples.getFirst().f();
        List<Intersection> asymptotes = asymptotes(inspected, av, bv);
        for (Sample sample : samples.subList(Math.min(1, samples.size()), samples.size())) {
            List<Intersection> others = asymptotes(sample.f(), av, bv);
            Optional<Double> outsideHere = outsideDomain(sample.f(), av, bv, others);
            String with = "Con " + other + " = " + fmtShort(sample.value()) + ", " + names.f();
            if (outsideHere.isPresent() || !samePoints(asymptotes, others)) {
                String where = outsideHere.isPresent()
                        ? " no está definida en " + variableName + " ≈ " + fmtShort(outsideHere.get())
                        : others.isEmpty()
                        ? " no tiene la asíntota de " + variableName + " = " + asymptotes.getFirst().value()
                        : " no está acotada cerca de " + variableName + " = " + others.getFirst().value();
                steps.add(new SolutionStep(with + where + ": los puntos problemáticos se mueven con "
                        + other + ", así que la integral depende de su valor."));
                return new CalculationResult(integral + ": depende de " + other, steps);
            }
        }
        Optional<Double> outside = outsideDomain(inspected, av, bv, asymptotes);
        if (outside.isPresent()) {
            steps.add(new SolutionStep(names.f() + " no está definida en " + variableName + " ≈ "
                    + fmt(outside.get()) + ", dentro de " + interval(a, b)
                    + ": el intervalo sale del dominio de f y la integral no tiene sentido."));
            return new CalculationResult(integral + ": fuera del dominio", steps,
                    graphable ? graph(f, a, b) : null);
        }
        explainWhyImproper(a, b, av, bv, asymptotes, names, steps);

        List<Piece> pieces = pieces(a, b, av, bv, asymptotes);
        if (pieces.size() > 1) {
            String sum = String.join(" + ",
                    pieces.stream().map(p -> "∫" + p.interval() + " " + names.integrand()).toList());
            steps.add(new SolutionStep("Se parte en tramos con un solo punto problemático cada uno: " + integral
                    + " = " + sum + ". Converge solo si todos los tramos convergen."));
        }

        steps.add(new SolutionStep("Buscar una antiderivada " + names.antiderivative() + " de " + names.f() + ":"));
        Optional<Expression> antiderivative;
        try {
            Expression found = indefinite.antiderivative(f, steps, 1, variableName);
            steps.add(new SolutionStep("Antiderivada: " + names.antiderivative() + " = " + found));
            antiderivative = Optional.of(found);
        } catch (UnsupportedIntegralException e) {
            if (parameterized) {
                // Simpson necesitaría un valor para el parámetro
                steps.add(new SolutionStep(e.getMessage()
                        + " Sin antiderivada no se puede estimar: el resultado depende de " + other + ".", 1));
                return new CalculationResult(integral + ": no se encontró antiderivada", steps);
            }
            steps.add(new SolutionStep(e.getMessage() + " Cada tramo se estima numéricamente.", 1));
            antiderivative = Optional.empty();
        }

        List<Limit> values = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            Piece piece = pieces.get(i);
            int level = pieces.size() > 1 ? 1 : 0;
            if (pieces.size() > 1) {
                steps.add(new SolutionStep("Tramo " + (i + 1) + ": ∫" + piece.interval() + " " + names.integrand()));
            }
            Limit value = antiderivative.isPresent()
                    ? exactPiece(antiderivative.get(), variable, piece, names, steps, level)
                    : numericPiece(f, piece, names, steps, level);
            values.add(value);
            if (!value.isFinite()) {
                // Un límite infinito diverge con cualquier valor del parámetro; uno que no existe puede depender de él
                if (parameterized && value.kind() == Limit.Kind.NONE) {
                    steps.add(new SolutionStep("No se puede concluir sin conocer el valor del parámetro "
                            + other + "."));
                    return new CalculationResult(integral + ": depende de " + other, steps);
                }
                String why = value.kind() == Limit.Kind.NONE ? "el límite no existe" : "el límite es " + value;
                String which = pieces.size() > 1 ? "Este tramo diverge (" + why + "), así que " + integral
                        + " diverge" : "Conclusión: " + why + ", así que la integral diverge";
                steps.add(new SolutionStep(which));
                return new CalculationResult(integral + " diverge" + (value.isInfinite() ? " (" + value + ")" : ""),
                        steps, graphable ? graph(f, a, b) : null);
            }
        }

        boolean exact = values.stream().allMatch(Limit::exact);
        String result;
        if (exact) {
            Expression total = values.getFirst().value();
            for (Limit value : values.subList(1, values.size())) {
                total = new Sum(total, value.value());
            }
            total = Simplifier.simplify(total);
            result = withApproximation(total);
        } else {
            result = "≈ " + fmt(values.stream().mapToDouble(Limit::approximation).sum());
        }
        String equals = exact ? " = " : " ";
        steps.add(new SolutionStep("Conclusión: el límite es un número, así que la integral converge: "
                + integral + equals + result));
        return new CalculationResult(integral + equals + result + " (converge)", steps,
                graphable ? graph(f, a, b) : null);
    }

    /**
     * Cómo se escriben f, F y el integrando según la variable:
     * {@code f(x)}, {@code f(y)} o {@code f(x,y)}, con {@code dx} o {@code dy}.
     *
     * @param arguments {@code (x)}, {@code (y)} o {@code (x,y)}
     */
    private record Names(String variable, String arguments) {
        String f() {
            return "f" + arguments;
        }

        String antiderivative() {
            return "F" + arguments;
        }

        /** {@code f(x) dx}. */
        String integrand() {
            return f() + " d" + variable;
        }
    }

    // ------------------------------------------------------------ parámetro

    /** Valores de prueba del parámetro: sin 0 ni enteros, para no caer en casos especiales. */
    private static final double[] PARAMETER_VALUES = {1.1, -0.6, 0.4, -1.3, 2.9, -2.7};

    /** f con el parámetro sustituido por un valor de prueba. */
    private record Sample(double value, Expression f) {
    }

    /**
     * Indica si una integral con parámetro es impropia: algún límite es
     * infinito o, con algún valor de prueba del parámetro, f tiene una
     * asíntota o sale del dominio en [a, b].
     */
    public static boolean isImproper(Expression f, Variable parameter, double a, double b) {
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return true;
        }
        double from = Math.min(a, b);
        double to = Math.max(a, b);
        return samples(f, parameter, from, to).stream().anyMatch(sample -> {
            List<Intersection> found = asymptotes(sample.f(), from, to);
            return !found.isEmpty() || outsideDomain(sample.f(), from, to, found).isPresent();
        });
    }

    /**
     * f con cada valor de prueba del parámetro, sin los valores con que f no
     * está definida en ningún punto de [a, b] (como √y con y < 0): esos
     * quedan fuera del dominio del parámetro. Si ninguno sirve, f con el
     * parámetro = 1.
     */
    private static List<Sample> samples(Expression f, Variable parameter, double a, double b) {
        double from = Double.isFinite(a) ? a : (Double.isFinite(b) ? b : 0) - SEARCH_WIDTH;
        double to = Double.isFinite(b) ? b : (Double.isFinite(a) ? a : 0) + SEARCH_WIDTH;
        List<Sample> samples = new ArrayList<>();
        for (double value : PARAMETER_VALUES) {
            Expression g = f.replace(parameter, new Constant(value));
            boolean defined = false;
            for (int i = 0; i <= 200 && !defined; i++) {
                defined = !Double.isNaN(g.evaluate(from + (to - from) * i / 200));
            }
            if (defined) {
                samples.add(new Sample(value, g));
            }
        }
        if (samples.isEmpty()) {
            samples.add(new Sample(1, f.replace(parameter, new Constant(1))));
        }
        return samples;
    }

    private static boolean samePoints(List<Intersection> first, List<Intersection> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (Math.abs(first.get(i).x() - second.get(i).x()) > 1e-6) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ tramos

    /**
     * Un tramo [from, to] con a lo sumo un extremo problemático.
     *
     * @param badFrom si el extremo izquierdo es -∞ o una asíntota
     * @param badTo   si el extremo derecho es ∞ o una asíntota
     */
    private record Piece(Expression from, Expression to, boolean badFrom, boolean badTo) {
        String interval() {
            return ImproperIntegral.interval(from, to);
        }
    }

    /**
     * Parte [a, b] en las asíntotas de adentro y, si un tramo queda con dos
     * extremos problemáticos, también en un punto intermedio.
     */
    private static List<Piece> pieces(Expression a, Expression b, double av, double bv, List<Intersection> asymptotes) {
        List<Expression> points = new ArrayList<>();
        List<Boolean> bad = new ArrayList<>();
        points.add(a);
        bad.add(Double.isInfinite(av) || asymptotes.stream().anyMatch(p -> near(p.x(), av)));
        for (Intersection asymptote : asymptotes) {
            if (!near(asymptote.x(), av) && !near(asymptote.x(), bv)) {
                points.add(asymptote.value());
                bad.add(true);
            }
        }
        points.add(b);
        bad.add(Double.isInfinite(bv) || asymptotes.stream().anyMatch(p -> near(p.x(), bv)));

        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            Expression from = points.get(i);
            Expression to = points.get(i + 1);
            if (bad.get(i) && bad.get(i + 1)) {
                Expression middle = middle(from, to);
                pieces.add(new Piece(from, middle, true, false));
                pieces.add(new Piece(middle, to, false, true));
            } else {
                pieces.add(new Piece(from, to, bad.get(i), bad.get(i + 1)));
            }
        }
        return pieces;
    }

    /** Un punto cómodo entre dos extremos: 0, el punto medio o uno a distancia 1. */
    private static Expression middle(Expression from, Expression to) {
        double f = from.evaluate(0);
        double t = to.evaluate(0);
        if (Double.isInfinite(f) && Double.isInfinite(t)) {
            return new Constant(0);
        }
        if (Double.isInfinite(f)) {
            return Simplifier.simplify(new Difference(to, new Constant(1)));
        }
        if (Double.isInfinite(t)) {
            return Simplifier.simplify(new Sum(from, new Constant(1)));
        }
        return Simplifier.simplify(new Quotient(new Sum(from, to), new Constant(2)));
    }

    /**
     * Tramo con antiderivada: lim t→b [F(t) - F(a)] o lim t→a [F(b) - F(t)].
     * Sin extremo problemático, es el Teorema Fundamental.
     */
    private Limit exactPiece(Expression antiderivative, Variable variable, Piece piece, Names names,
                             List<SolutionStep> steps, int level) {
        Expression atT = antiderivative.replace(variable, T);
        if (!piece.badFrom() && !piece.badTo()) {
            Expression value = Simplifier.simplify(new Difference(antiderivative.replace(variable, piece.to()),
                    antiderivative.replace(variable, piece.from())));
            steps.add(new SolutionStep("Sin puntos problemáticos: F(" + piece.to() + ") - F(" + piece.from() + ") = "
                    + withApproximation(value), level));
            return Limit.finite(value);
        }
        LimitPoint target;
        Expression difference;
        String inner;
        if (piece.badTo()) {
            target = Double.isInfinite(piece.to().evaluate(0)) ? LimitPoint.positiveInfinity()
                    : LimitPoint.fromLeft(piece.to());
            difference = new Difference(atT, antiderivative.replace(variable, piece.from()));
            inner = "[F(t) - F(" + piece.from() + ")]";
        } else {
            target = Double.isInfinite(piece.from().evaluate(0)) ? LimitPoint.negativeInfinity()
                    : LimitPoint.fromRight(piece.from());
            difference = new Difference(antiderivative.replace(variable, piece.to()), atT);
            inner = "[F(" + piece.to() + ") - F(t)]";
        }
        Expression g = Simplifier.simplify(difference);
        String lim = "lim " + target.describe("t");
        String partial = piece.badTo() ? "∫[" + piece.from() + ", t]" : "∫[t, " + piece.to() + "]";
        steps.add(new SolutionStep("Como límite: ∫" + piece.interval() + " " + names.integrand() + " = " + lim + " "
                + partial + " " + names.integrand() + " = " + lim + " " + inner + " = " + lim + " (" + g + ")",
                level));
        Limit value = limits.limit(g, T, target, steps, level + 1);
        steps.add(new SolutionStep(lim + " (" + g + ") = " + value, level));
        return value;
    }

    /**
     * Tramo sin antiderivada: se integra con Simpson hasta t cada vez más
     * cerca del extremo problemático y se observa si el valor se estabiliza.
     */
    private static Limit numericPiece(Expression f, Piece piece, Names names, List<SolutionStep> steps,
                                      int level) {
        double from = piece.from().evaluate(0);
        double to = piece.to().evaluate(0);
        if (!piece.badFrom() && !piece.badTo()) {
            double value = simpson(f, from, to);
            steps.add(new SolutionStep("Sin puntos problemáticos (Simpson): ≈ " + fmt(value), level));
            return Limit.approximate(value);
        }
        boolean upper = piece.badTo();
        double fixed = upper ? from : to;
        double bad = upper ? to : from;
        double direction = upper ? 1 : -1;
        steps.add(new SolutionStep("Se estima con Simpson, acercando t al extremo "
                + (upper ? piece.to() : piece.from()) + ":", level));

        double total = 0;
        double previous = fixed;
        double lastIncrement = Double.NaN;
        double increment = Double.NaN;
        for (int k = 0; k < NUMERIC_STEPS; k++) {
            double t = Double.isInfinite(bad)
                    ? fixed + direction * Math.pow(10, k)
                    : bad - direction * Math.abs(bad - fixed) / 2 * Math.pow(10, -k);
            lastIncrement = increment;
            double part = simpson(f, Math.min(previous, t), Math.max(previous, t));
            increment = Math.abs(part);
            total += part;
            previous = t;
            String integral = upper ? "∫[" + fmtShort(fixed) + ", " + fmtShort(t) + "]"
                    : "∫[" + fmtShort(t) + ", " + fmtShort(fixed) + "]";
            steps.add(new SolutionStep("t = " + fmtShort(t) + ":   " + integral + " " + names.integrand() + " ≈ "
                    + fmt(total),
                    level + 1));
        }
        boolean settles = Double.isFinite(total) && increment < 1e-4 * (1 + Math.abs(total))
                && increment <= lastIncrement;
        Limit value = settles ? Limit.approximate(total) : Limit.infinity(total);
        steps.add(new SolutionStep(settles
                ? "Los valores se estabilizan: ≈ " + fmt(total)
                : "Los valores siguen creciendo: el tramo diverge", level));
        return value;
    }

    // ------------------------------------------------------------ apoyo

    /** Explica por qué es impropia: tipo 1 (intervalo infinito) o tipo 2 (asíntota). */
    private static void explainWhyImproper(Expression a, Expression b, double av, double bv,
                                           List<Intersection> asymptotes, Names names,
                                           List<SolutionStep> steps) {
        List<String> reasons = new ArrayList<>();
        if (Double.isInfinite(av) && Double.isInfinite(bv)) {
            reasons.add("los dos límites son infinitos");
        } else if (Double.isInfinite(bv)) {
            reasons.add("el límite superior es ∞");
        } else if (Double.isInfinite(av)) {
            reasons.add("el límite inferior es -∞");
        }
        String type1 = reasons.isEmpty() ? "" : "Integral impropia de tipo 1 (intervalo infinito): "
                + reasons.getFirst() + ".";
        String places = String.join(", ", asymptotes.stream().map(p -> names.variable() + " = " + p.value()).toList());
        String type2 = asymptotes.isEmpty() ? "" : "Integral impropia de tipo 2: " + names.f()
                + " no está acotada cerca de "
                + places + " (asíntota vertical).";
        steps.add(new SolutionStep((type1 + " " + type2).trim()));
    }

    /**
     * Primer punto (lejos de las asíntotas) donde f no está definida, como
     * √x en [-1, 1].
     */
    private static Optional<Double> outsideDomain(Expression f, double a, double b, List<Intersection> asymptotes) {
        double from = Double.isFinite(a) ? a : (Double.isFinite(b) ? b : 0) - SEARCH_WIDTH;
        double to = Double.isFinite(b) ? b : (Double.isFinite(a) ? a : 0) + SEARCH_WIDTH;
        int samples = 2001;
        for (int i = 0; i < samples; i++) {
            double x = from + (to - from) * i / (samples - 1);
            boolean nearAsymptote = asymptotes.stream().anyMatch(p -> Math.abs(p.x() - x) < 1e-6);
            if (Double.isNaN(f.evaluate(x)) && !nearAsymptote) {
                return Optional.of(x);
            }
        }
        return Optional.empty();
    }

    /** {@code [1, ∞)}, {@code (-∞, 0]}, {@code (-∞, ∞)}, {@code [0, 1]}. */
    static String interval(Expression a, Expression b) {
        String left = Double.isInfinite(a.evaluate(0)) ? "(" : "[";
        String right = Double.isInfinite(b.evaluate(0)) ? ")" : "]";
        return left + a + ", " + b + right;
    }

    /** La curva y el área en la parte finita que se ve. */
    private static Graph graph(Expression f, Expression a, Expression b) {
        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        double from = Double.isFinite(av) ? av : (Double.isFinite(bv) ? bv - 10 : -10);
        double to = Double.isFinite(bv) ? bv : (Double.isFinite(av) ? av + 10 : 10);
        Range xRange = Range.centeredAt((from + to) / 2, Math.max((to - from) * 1.3, 2));
        double shadeFrom = Double.isFinite(av) ? av : xRange.min();
        double shadeTo = Double.isFinite(bv) ? bv : xRange.max();
        GraphArea area = new GraphArea("Área: ∫" + interval(a, b) + " f(x) dx", f, shadeFrom, shadeTo);
        return new Graph(List.of(new GraphCurve("f(x) = " + f, f, CurveStyle.FUNCTION)), List.of(), xRange,
                List.of(area), List.of());
    }

    private static double simpson(Expression f, double a, double b) {
        try {
            return NumericalMethod.SIMPSON.getRule().integrate(f, a, b, SIMPSON_SUBINTERVALS).value();
        } catch (ParameterException impossible) {
            return Double.NaN;
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9 * (1 + Math.abs(b));
    }

    /** {@code 1/3 ≈ 0.3333333333}, o solo el número si ya es un número. */
    private static String withApproximation(Expression e) {
        if (e.dependsOnVariable()) {
            return e.toString();
        }

        boolean plainNumber = e instanceof Constant c && c.isNumber();

        return plainNumber
                ? e.toString()
                : e + " ≈ " + fmt(e.evaluate(0));
    }

    private static String fmt(double value) {
        return FormatUtils.formatNumber(value);
    }

    private static String fmtShort(double value) {
        return FormatUtils.formatShort(value);
    }
}
