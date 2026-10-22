package com.callista.model.calculus.area;

import com.callista.model.calculus.area.CurveIntersections.Intersection;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Simplifier;
import com.callista.model.graph.Range;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Región del plano entre dos curvas f y g, con sus puntos de corte: la que
 * encierran (del primer al último corte) o la de un intervalo [a, b].
 *
 * @param f      curva f(x)
 * @param g      curva g(x) (0 si es el eje x)
 * @param h      f − g simplificada
 * @param limits extremos y cortes de adentro, de izquierda a derecha: los
 *               tramos van entre dos seguidos
 */
public record Region(Expression f, Expression g, Expression h, List<Intersection> limits) {

    /** Intervalo donde se buscan los cortes cuando no se dan a y b. */
    public static final Range SEARCH_RANGE = new Range(-10, 10);
    /** Con más cortes que estos se piden a y b. */
    private static final int MAX_AUTOMATIC_INTERSECTIONS = 4;

    public Region {
        limits = List.copyOf(limits);
    }

    /**
     * La región que encierran f y g, del primer al último corte, explicando
     * en {@code steps} cómo se encontraron.
     *
     * @throws ParameterException si no encierran una región
     */
    public static Region enclosed(Expression f, Expression g, List<SolutionStep> steps) throws ParameterException {
        Expression h = difference(f, g);
        List<Intersection> cuts = CurveIntersections.find(h, SEARCH_RANGE.min(), SEARCH_RANGE.max());
        if (cuts.size() < 2) {
            String where = cuts.isEmpty() ? "no se cortan" : "solo se cortan en x = " + cuts.getFirst().value();
            throw new ParameterException("Entre x = " + fmt(SEARCH_RANGE.min()) + " y x = " + fmt(SEARCH_RANGE.max())
                    + " las curvas " + where + ", así que no encierran una región: escribe a y b.");
        }
        if (cuts.size() > MAX_AUTOMATIC_INTERSECTIONS) {
            throw new ParameterException("f y g se cortan " + cuts.size() + " veces entre x = "
                    + fmt(SEARCH_RANGE.min()) + " y x = " + fmt(SEARCH_RANGE.max())
                    + ": escribe a y b para elegir la región.");
        }
        describeCurves(f, g, steps);
        steps.add(new SolutionStep("Puntos de corte: f(x) = g(x) ⇔ f(x) - g(x) = " + h + " = 0"));
        for (Intersection cut : cuts) {
            steps.add(new SolutionStep(describe(cut, f), 1));
        }
        steps.add(new SolutionStep("La región encerrada va del primer al último corte: [" + cuts.getFirst().value()
                + ", " + cuts.getLast().value() + "]"));
        return new Region(f, g, h, cuts);
    }

    /**
     * La región entre f y g en [a, b]; los cortes de adentro la parten en tramos.
     *
     * @throws ParameterException si a no es menor que b, o si f y g son iguales
     */
    public static Region between(Expression f, Expression g, Expression a, Expression b, List<SolutionStep> steps)
            throws ParameterException {
        double av = a.evaluate(0);
        double bv = b.evaluate(0);
        if (!(av < bv)) {
            throw new ParameterException("a debe ser menor que b.");
        }
        Expression h = difference(f, g);
        describeCurves(f, g, steps);
        List<Intersection> inside = CurveIntersections.find(h, av, bv).stream()
                .filter(cut -> !near(cut.x(), av) && !near(cut.x(), bv))
                .toList();
        if (inside.isEmpty()) {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): ninguno"));
        } else {
            steps.add(new SolutionStep("Puntos de corte dentro de (" + a + ", " + b + "): f(x) - g(x) = " + h
                    + " = 0"));
            for (Intersection cut : inside) {
                steps.add(new SolutionStep(describe(cut, f), 1));
            }
        }
        List<Intersection> limits = new ArrayList<>();
        limits.add(new Intersection(av, a, true));
        limits.addAll(inside);
        limits.add(new Intersection(bv, b, true));
        return new Region(f, g, h, limits);
    }

    /** Extremo izquierdo. */
    public Intersection first() {
        return limits.getFirst();
    }

    /** Extremo derecho. */
    public Intersection last() {
        return limits.getLast();
    }

    /** {@code [0, 1]}. */
    public String interval() {
        return "[" + first().value() + ", " + last().value() + "]";
    }

    /** Si g es el eje x. */
    public boolean isAgainstXAxis() {
        return g instanceof Constant c && c.isNumber(0);
    }

    private static void describeCurves(Expression f, Expression g, List<SolutionStep> steps) {
        boolean axis = g instanceof Constant c && c.isNumber(0);
        steps.add(new SolutionStep("Región entre f(x) = " + f + " y g(x) = " + g + (axis ? " (el eje x)" : "")));
    }

    /** {@code x = 1  →  (1, 1)}. */
    private static String describe(Intersection cut, Expression f) {
        String exact = cut.exact() ? "" : " (aproximado)";
        String approx = cut.exact() && !(cut.value() instanceof Constant c && c.isNumber())
                ? " ≈ " + FormatUtils.formatShort(cut.x()) : "";
        double y = f.evaluate(cut.x());
        return "x = " + cut.value() + approx + exact + "  →  (" + FormatUtils.formatShort(cut.x()) + ", "
                + FormatUtils.formatShort(y) + ")";
    }

    private static Expression difference(Expression f, Expression g) throws ParameterException {
        Expression h = Simplifier.simplify(new Difference(f, g));
        if (h instanceof Constant c && c.isNumber(0)) {
            throw new ParameterException("f y g son la misma función: no hay región entre ellas.");
        }
        return h;
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9 * (1 + Math.abs(b));
    }

    private static String fmt(double value) {
        return FormatUtils.formatNumber(value);
    }
}
