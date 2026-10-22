package com.callista.model.calculus.revolution;

import com.callista.model.calculus.area.CurveIntersections;
import com.callista.model.calculus.area.CurveIntersections.Intersection;
import com.callista.model.calculus.area.Region;
import com.callista.model.calculus.integral.IndefiniteIntegral;
import com.callista.model.calculus.revolution.VolumeMethod.Curve;
import com.callista.model.calculus.revolution.VolumeMethod.Slice;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.GraphLine;
import com.callista.model.entity.GraphPoint;
import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.GraphPolygon.Vertex;
import com.callista.model.entity.Solid;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.ParameterException;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Product;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;
import com.callista.model.graph.Range;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Volumen del sólido que se obtiene al girar una región (entre f y g)
 * alrededor de una recta.
 *
 * <p>Como las curvas están en función de x, las rebanadas son verticales
 * (de ancho dx):</p>
 * <ul>
 *     <li>con un eje <b>horizontal</b> (y = k) son perpendiculares al eje y
 *     generan <b>discos</b> (región pegada al eje) o <b>arandelas</b>;</li>
 *     <li>con un eje <b>vertical</b> (x = k) son paralelas al eje y generan
 *     <b>capas cilíndricas</b>.</li>
 * </ul>
 *
 * <p>Cada tramo se integra con el Teorema Fundamental (o con Simpson si no
 * hay antiderivada elemental) y el resultado se comprueba con una
 * integración numérica independiente.</p>
 */
public class SolidOfRevolution {

    /** Subintervalos de Simpson para aproximar un tramo o comprobar el resultado. */
    private static final int SIMPSON_SUBINTERVALS = 2000;
    /** Subintervalos de la comprobación (más, porque √x cerca de 0 converge lento). */
    private static final int CHECK_SUBINTERVALS = 40_000;
    private static final Variable X = new Variable("x");

    private final IndefiniteIntegral indefinite = new IndefiniteIntegral();

    /**
     * Volumen al girar la región que encierran f y g.
     *
     * @throws ParameterException si no encierran una región, o si el eje la atraviesa
     */
    public CalculationResult enclosed(Expression f, Expression g, RotationAxis axis) throws ParameterException {
        List<SolutionStep> steps = new ArrayList<>();
        return volume(Region.enclosed(f, g, steps), axis, steps);
    }

    /**
     * Volumen al girar la región entre f y g en [a, b].
     *
     * @throws ParameterException si a no es menor que b, o si el eje vertical
     *                            atraviesa la región
     */
    public CalculationResult between(Expression f, Expression g, Expression a, Expression b, RotationAxis axis)
            throws ParameterException {
        List<SolutionStep> steps = new ArrayList<>();
        return volume(Region.between(f, g, a, b, steps), axis, steps);
    }

    private CalculationResult volume(Region region, RotationAxis axis, List<SolutionStep> steps)
            throws ParameterException {
        double k = axis.value();
        if (axis.vertical() && k > region.first().x() + 1e-12 && k < region.last().x() - 1e-12) {
            throw new ParameterException("El eje " + axis.equation() + " atraviesa la región " + region.interval()
                    + ": elige un eje vertical a la izquierda o a la derecha de la región.");
        }
        VolumeMethod method = chooseMethod(region, axis);
        steps.add(new SolutionStep("Eje de giro: " + axis + ". " + reason(axis, method)));
        steps.add(new SolutionStep("Método: " + method.getName() + ", " + method.getFormula()));

        List<Intersection> limits = axis.vertical() ? region.limits() : withAxisCrossings(region, axis);
        Expression factor = method.getFactor();
        List<Part> parts = new ArrayList<>();
        int count = limits.size() - 1;
        for (int i = 0; i < count; i++) {
            parts.add(part(region, axis, method, limits.get(i), limits.get(i + 1), i + 1, count > 1, steps));
        }

        boolean exact = parts.stream().allMatch(p -> p.exact() != null);
        double total = parts.stream().mapToDouble(Part::value).sum() * factor.evaluate(0);
        String result;
        if (exact) {
            Expression sum = parts.getFirst().exact();
            for (Part p : parts.subList(1, parts.size())) {
                sum = new Sum(sum, p.exact());
            }
            Expression volume = tidy(new Product(factor, sum));
            String names = count > 1 ? factor + "·(" + String.join(" + ",
                    parts.stream().map(Part::name).toList()) + ") = " : "";
            result = withApproximation(volume);
            steps.add(new SolutionStep("Volumen: V = " + names + result));
        } else {
            result = "≈ " + fmt(total);
            steps.add(new SolutionStep("Volumen: V " + result));
        }

        double check = independentCheck(region, axis);
        if (Double.isFinite(check)) {
            boolean agrees = Math.abs(check - total) <= 1e-5 * Math.max(1, Math.abs(total));
            steps.add(new SolutionStep("Comprobación numérica (Simpson sobre " + (axis.vertical()
                    ? "2π·|x - k|·|f - g|" : "π·(R² - r²) punto por punto") + "): ≈ " + fmt(check)
                    + (agrees ? " ✓" : " ✗ (no coincide)")));
        }

        String summary = "V = " + (exact ? result : fmt(total));
        if (!exact) {
            summary = "V ≈ " + fmt(total);
        }
        Solid solid = new Solid(parts.stream().map(p -> new Solid.Piece(p.from().x(), p.to().x(),
                p.upper().expression(), p.lower().expression())).toList(), axis.vertical(), k, summary);
        return new CalculationResult(summary, steps, graph(region, axis, parts), solid);
    }

    /**
     * Un tramo ya integrado.
     *
     * @param exact el valor exacto de la integral (sin el factor), o {@code null} si se aproximó
     * @param value su valor numérico (sin el factor)
     */
    private record Part(int index, boolean numbered, Intersection from, Intersection to, Curve upper, Curve lower,
                        Expression exact, double value) {
        String name() {
            return numbered ? "I" + FormatUtils.subscript(index) : "I";
        }
    }

    private Part part(Region region, RotationAxis axis, VolumeMethod method, Intersection from, Intersection to,
                      int index, boolean numbered, List<SolutionStep> steps) {
        double middle = (from.x() + to.x()) / 2;
        boolean fOnTop = region.h().evaluate(middle) >= 0;
        Curve f = new Curve("f(x)", region.f());
        Curve g = new Curve("g(x)", region.g());
        Curve upper = fOnTop ? f : g;
        Curve lower = fOnTop ? g : f;
        Slice slice = method.slice(upper, lower, axis, middle);
        String interval = "[" + from.value() + ", " + to.value() + "]";
        String name = numbered ? "I" + FormatUtils.subscript(index) : "I";
        steps.add(new SolutionStep((numbered ? "Tramo " + index + " " : "En ") + interval + ": "
                + upper.name() + " va arriba; " + slice.description()));
        steps.add(new SolutionStep(name + " = ∫" + interval + " (" + slice.integrand() + ") dx", 1));

        try {
            Expression antiderivative = indefinite.antiderivative(slice.integrand(), steps, 2);
            Expression atTo = tidy(antiderivative.replace(X, to.value()));
            Expression atFrom = tidy(antiderivative.replace(X, from.value()));
            Expression exact = tidy(new Difference(atTo, atFrom));
            steps.add(new SolutionStep("Antiderivada: " + antiderivative + ";  " + name + " = "
                    + withApproximation(exact), 1));
            boolean exactLimits = from.exact() && to.exact();
            return new Part(index, numbered, from, to, upper, lower, exactLimits ? exact : null, exact.evaluate(0));
        } catch (UnsupportedIntegralException e) {
            double value = simpson(slice.integrand()::evaluate, from.x(), to.x(), SIMPSON_SUBINTERVALS);
            steps.add(new SolutionStep("Sin antiderivada elemental: " + name + " ≈ " + fmt(value)
                    + " (Simpson, n = " + SIMPSON_SUBINTERVALS + ")", 1));
            return new Part(index, numbered, from, to, upper, lower, null, value);
        }
    }

    // ------------------------------------------------------------ método

    /**
     * Eje vertical: capas. Eje horizontal: discos si una de las curvas es
     * el propio eje (la región está pegada a él), si no arandelas.
     */
    private static VolumeMethod chooseMethod(Region region, RotationAxis axis) {
        if (axis.vertical()) {
            return new ShellMethod();
        }
        boolean touchesAxis = Radius.isOnAxis(new Curve("g(x)", region.g()), axis)
                || Radius.isOnAxis(new Curve("f(x)", region.f()), axis);
        return touchesAxis ? new DiskMethod() : new WasherMethod();
    }

    private static String reason(RotationAxis axis, VolumeMethod method) {
        if (axis.vertical()) {
            return "Las rebanadas verticales (dx) son paralelas al eje: al girar, cada una forma una capa cilíndrica.";
        }
        String shape = method instanceof DiskMethod ? "un disco (la región está pegada al eje)"
                : "una arandela (un disco con hueco)";
        return "Las rebanadas verticales (dx) son perpendiculares al eje: al girar, cada una forma " + shape + ".";
    }

    /**
     * Con un eje horizontal, los radios pueden cambiar de curva donde una
     * curva cruza el eje o donde las dos quedan a la misma distancia de lados
     * opuestos: ahí también se parte.
     */
    private static List<Intersection> withAxisCrossings(Region region, RotationAxis axis) {
        Expression k = axis.k();
        List<Expression> switches = List.of(
                Simplifier.simplify(new Difference(region.f(), k)),
                Simplifier.simplify(new Difference(region.g(), k)),
                Simplifier.simplify(new Difference(new Sum(region.f(), region.g()),
                        new Product(new Constant(2), k))));
        List<Intersection> limits = new ArrayList<>(region.limits());
        for (int i = 0; i + 1 < region.limits().size(); i++) {
            double from = region.limits().get(i).x();
            double to = region.limits().get(i + 1).x();
            for (Expression s : switches) {
                if (!s.dependsOnVariable()) {
                    continue;
                }
                for (Intersection cut : CurveIntersections.find(s, from, to)) {
                    boolean known = limits.stream().anyMatch(l -> Math.abs(l.x() - cut.x()) < 1e-7);
                    if (!known && cut.x() > from && cut.x() < to) {
                        limits.add(cut);
                    }
                }
            }
        }
        limits.sort((p, q) -> Double.compare(p.x(), q.x()));
        return limits;
    }

    /**
     * Volumen calculado de otra forma, punto por punto y sin elegir curvas ni
     * tramos, para comprobar el resultado.
     */
    private static double independentCheck(Region region, RotationAxis axis) {
        double k = axis.value();
        Expression f = region.f();
        Expression g = region.g();
        DoubleUnaryOperator integrand;
        double factor;
        if (axis.vertical()) {
            integrand = x -> Math.abs(x - k) * Math.abs(f.evaluate(x) - g.evaluate(x));
            factor = 2 * Math.PI;
        } else {
            integrand = x -> {
                double a = f.evaluate(x) - k;
                double b = g.evaluate(x) - k;
                double outer = Math.max(Math.abs(a), Math.abs(b));
                double inner = a * b > 0 ? Math.min(Math.abs(a), Math.abs(b)) : 0;
                return outer * outer - inner * inner;
            };
            factor = Math.PI;
        }
        double total = 0;
        for (int i = 0; i + 1 < region.limits().size(); i++) {
            total += simpson(integrand, region.limits().get(i).x(), region.limits().get(i + 1).x(),
                    CHECK_SUBINTERVALS);
        }
        return factor * total;
    }

    // ------------------------------------------------------------ gráfica

    /**
     * La región, el eje y un rectángulo representativo con su reflejo al
     * otro lado del eje (lo que barre al girar).
     */
    private static Graph graph(Region region, RotationAxis axis, List<Part> parts) {
        double k = axis.value();
        List<GraphCurve> curves = new ArrayList<>(List.of(
                new GraphCurve("f(x) = " + region.f(), region.f(), CurveStyle.FUNCTION),
                new GraphCurve("g(x) = " + region.g(), region.g(), CurveStyle.SECOND_FUNCTION)));
        List<GraphLine> lines = new ArrayList<>();
        String axisLabel = "Eje de giro: " + axis.equation();
        if (axis.vertical()) {
            lines.add(new GraphLine(axisLabel, k));
        } else {
            curves.add(new GraphCurve(axisLabel, axis.k(), CurveStyle.TANGENT));
        }

        List<GraphPoint> points = new ArrayList<>();
        for (Intersection cut : region.limits()) {
            double y = region.f().evaluate(cut.x());
            if (Double.isFinite(y) && Math.abs(y - region.g().evaluate(cut.x())) < 1e-7) {
                points.add(new GraphPoint("(" + FormatUtils.formatShort(cut.x()) + ", "
                        + FormatUtils.formatShort(y) + ")", cut.x(), y));
            }
        }

        List<GraphArea> areas = parts.stream().map(p -> new GraphArea("Región que gira", p.upper().expression(),
                p.lower().expression(), p.from().x(), p.to().x())).toList();

        Part widest = parts.stream().max((p, q) -> Double.compare(p.to().x() - p.from().x(),
                q.to().x() - q.from().x())).orElseThrow();
        double middle = (widest.from().x() + widest.to().x()) / 2;
        double half = (widest.to().x() - widest.from().x()) / 14;
        double top = widest.upper().expression().evaluate(middle);
        double bottom = widest.lower().expression().evaluate(middle);
        List<GraphPolygon> polygons = new ArrayList<>();
        if (Double.isFinite(top) && Double.isFinite(bottom)) {
            polygons.add(rectangle(middle - half, middle + half, bottom, top));
            polygons.add(axis.vertical()
                    ? rectangle(2 * k - middle - half, 2 * k - middle + half, bottom, top)
                    : rectangle(middle - half, middle + half, 2 * k - top, 2 * k - bottom));
        }

        double a = region.first().x();
        double b = region.last().x();
        double from = axis.vertical() ? Math.min(a, 2 * k - b) : a;
        double to = axis.vertical() ? Math.max(b, 2 * k - a) : b;
        Range xRange = Range.centeredAt((from + to) / 2, Math.max((to - from) * 1.4, 2));
        return new Graph(curves, points, xRange, areas, polygons, lines);
    }

    private static GraphPolygon rectangle(double x1, double x2, double y1, double y2) {
        return new GraphPolygon(List.of(new Vertex(x1, y1), new Vertex(x2, y1), new Vertex(x2, y2),
                new Vertex(x1, y2)));
    }

    // ------------------------------------------------------------ apoyo

    /**
     * La forma más corta entre la simplificada y la desarrollada.
     */
    private static Expression tidy(Expression e) {
        Expression simplified = Simplifier.simplify(e);
        Expression expanded = Simplifier.expand(e);
        return expanded.toString().length() < simplified.toString().length() ? expanded : simplified;
    }

    private static double simpson(DoubleUnaryOperator f, double a, double b, int n) {
        double h = (b - a) / n;
        double sum = f.applyAsDouble(a) + f.applyAsDouble(b);
        for (int i = 1; i < n; i++) {
            sum += (i % 2 == 0 ? 2 : 4) * f.applyAsDouble(a + i * h);
        }
        return sum * h / 3;
    }

    private static String withApproximation(Expression e) {
        boolean plainNumber = e instanceof Constant c && c.isNumber();
        return plainNumber ? e.toString() : e + " ≈ " + fmt(e.evaluate(0));
    }

    private static String fmt(double value) {
        return FormatUtils.formatNumber(value);
    }
}
