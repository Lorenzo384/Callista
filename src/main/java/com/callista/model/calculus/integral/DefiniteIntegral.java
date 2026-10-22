package com.callista.model.calculus.integral;

import com.callista.model.calculus.numeric.NumericalIntegration;
import com.callista.model.calculus.numeric.NumericalIntegration.Node;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.calculus.numeric.WeightedRule;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.GraphPolygon;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.model.graph.Range;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Integral definida {@code ∫[a, b] f(x) dx}: arma el resultado, el
 * procedimiento y la gráfica.
 *
 * <p>Por ahora con métodos numéricos (trapecio, Simpson, punto medio) y la
 * comparación entre ellos. El cálculo exacto con el Teorema Fundamental del
 * Cálculo llegará con las integrales simbólicas.</p>
 */
public class DefiniteIntegral {

    /** Subintervalos del valor de referencia con que se estima el error. */
    public static final int REFERENCE_SUBINTERVALS = 10_000;
    /** Si hay más puntos que estos, la tabla del procedimiento se resume. */
    private static final int MAX_LISTED_NODES = 12;
    /** Con más subintervalos que estos, las figuras no se dibujan (serían demasiado finas). */
    private static final int MAX_DRAWN_SHAPES = 60;

    /**
     * Aproxima la integral con un método y explica cada paso.
     *
     * @throws ParameterException si {@code n} no es válido para el método
     */
    public CalculationResult approximate(Expression f, double a, double b, NumericalMethod method, int n)
            throws ParameterException {
        WeightedRule rule = method.getRule();
        NumericalIntegration result = rule.integrate(f, a, b, n);

        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep("Función: f(x) = " + f));
        steps.add(new SolutionStep(rule.getName() + " con n = " + n + ": "
                + integral(a, b) + " ≈ " + rule.getFormula()));
        steps.add(new SolutionStep("Ancho de cada subintervalo: Δx = (b - a)/n = ("
                + fmt(b) + " - " + signed(a) + ")/" + n + " = " + fmt(result.dx())));
        if (!result.isDefined()) {
            return undefined(f, a, b, result, steps);
        }

        steps.add(new SolutionStep("Evaluar f en los " + result.nodes().size()
                + " puntos y multiplicar cada valor por su peso:"));
        for (String line : nodeTable(rule, result.nodes())) {
            steps.add(new SolutionStep(line, 1));
        }
        String sum = fmt(result.weightedSum());
        String symbol = rule.nodeSymbol();
        steps.add(new SolutionStep("Suma ponderada: Σ pesoᵢ·f(" + symbol + "ᵢ) = " + sum));
        steps.add(new SolutionStep("Resultado: " + rule.factorText() + "·Σ = " + factorText(rule, result.dx())
                + "·" + sum + " = " + fmt(result.value())));

        double reference = reference(f, a, b);
        if (Double.isFinite(reference)) {
            steps.add(new SolutionStep("Valor de referencia (Simpson con n = " + REFERENCE_SUBINTERVALS + "): "
                    + fmt(reference) + "  →  error estimado ≈ " + error(result.value(), reference)));
        } else {
            steps.add(improperWarning(a, b));
        }

        List<GraphPolygon> shapes = n <= MAX_DRAWN_SHAPES ? finiteShapes(rule.shapes(f, a, b, n)) : List.of();
        return new CalculationResult(integral(a, b) + " ≈ " + fmt(result.value()), steps,
                graph(f, a, b, shapes));
    }

    /**
     * Aplica los tres métodos con el mismo {@code n}, estima el error de cada
     * uno y dice cuál fue el más preciso.
     *
     * @throws ParameterException si {@code n} no es válido
     */
    public CalculationResult compare(Expression f, double a, double b, int n) throws ParameterException {
        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep("Función: f(x) = " + f));
        steps.add(new SolutionStep("Comparación de métodos con n = " + n
                + " subintervalos (Δx = " + fmt((b - a) / n) + "):"));

        // Simpson necesita n par: si no lo es, se usa el siguiente
        int simpsonN = n % 2 == 0 ? n : n + 1;
        List<NumericalIntegration> results = List.of(
                NumericalMethod.TRAPEZOIDAL.getRule().integrate(f, a, b, n),
                NumericalMethod.MIDPOINT.getRule().integrate(f, a, b, n),
                NumericalMethod.SIMPSON.getRule().integrate(f, a, b, simpsonN));
        for (NumericalIntegration result : results) {
            if (!result.isDefined()) {
                return undefined(f, a, b, result, steps);
            }
        }

        double reference = reference(f, a, b);
        for (NumericalIntegration result : results) {
            String error = Double.isFinite(reference)
                    ? "   (error ≈ " + error(result.value(), reference) + ")"
                    : "";
            String note = result.subintervals() != n ? " con n = " + result.subintervals() + " (necesita n par)" : "";
            steps.add(new SolutionStep(result.rule().getShortName() + note + ": ≈ " + fmt(result.value()) + error, 1));
        }

        NumericalIntegration best = results.getLast();
        if (Double.isFinite(reference)) {
            steps.add(new SolutionStep("Valor de referencia (Simpson con n = " + REFERENCE_SUBINTERVALS + "): "
                    + fmt(reference)));
            best = results.stream()
                    .min(Comparator.comparingDouble(r -> Math.abs(r.value() - reference)))
                    .orElseThrow();
            steps.add(new SolutionStep("Conclusión: el más preciso aquí es " + best.rule().getShortName()
                    + ". Simpson aproxima la curva con parábolas; el trapecio y el punto medio, con rectas, "
                    + "y sus errores suelen tener signos contrarios."));
        } else {
            steps.add(improperWarning(a, b));
        }
        String summary = integral(a, b) + " ≈ " + fmt(best.value()) + " (" + best.rule().getShortName() + ")";
        return new CalculationResult(summary, steps, graph(f, a, b, List.of()));
    }

    // ------------------------------------------------------------ apoyo

    /**
     * Resultado cuando f no está definida en algún punto del intervalo.
     */
    private static CalculationResult undefined(Expression f, double a, double b, NumericalIntegration result,
                                               List<SolutionStep> steps) {
        double x = result.firstUndefinedNode().map(Node::x).orElse(Double.NaN);
        steps.add(new SolutionStep("f(x) no está definida en x = " + fmt(x) + ", que está dentro de ["
                + fmt(Math.min(a, b)) + ", " + fmt(Math.max(a, b)) + "]. Los métodos numéricos necesitan evaluar f "
                + "en todos los puntos: prueba con otro intervalo."));
        return new CalculationResult(integral(a, b) + ": no se puede aproximar", steps,
                new Graph(List.of(curve(f)), List.of(), xRange(a, b)));
    }

    /**
     * Advertencia cuando f no está definida en algún punto del intervalo
     * aunque el método no lo haya tocado: por ejemplo, {@code 1/x} en
     * [-1, 1]. La integral es impropia y la aproximación no es confiable.
     */
    private static SolutionStep improperWarning(double a, double b) {
        return new SolutionStep("Advertencia: f(x) no está definida en todo [" + fmt(Math.min(a, b)) + ", "
                + fmt(Math.max(a, b)) + "] (tiene una asíntota o un punto fuera de su dominio). "
                + "La integral es impropia y esta aproximación no es confiable.");
    }

    /**
     * Valor muy preciso (Simpson con muchos subintervalos) para estimar el
     * error; {@code NaN} si no se puede calcular.
     */
    private static double reference(Expression f, double a, double b) {
        try {
            return NumericalMethod.SIMPSON.getRule().integrate(f, a, b, REFERENCE_SUBINTERVALS).value();
        } catch (ParameterException impossible) {
            return Double.NaN;
        }
    }

    /**
     * Una línea por punto: {@code x₁ = 0.5    f(x₁) = 0.25    peso 2}. Si
     * hay muchos, se muestran los primeros y los últimos.
     */
    private static List<String> nodeTable(WeightedRule rule, List<Node> nodes) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            boolean hidden = nodes.size() > MAX_LISTED_NODES && i >= 5 && i < nodes.size() - 3;
            if (hidden) {
                if (i == 5) {
                    lines.add("⋮");
                }
                continue;
            }
            Node node = nodes.get(i);
            String name = rule.nodeSymbol() + FormatUtils.subscript(node.index());
            lines.add(String.format("%-20s %-30s peso %d",
                    name + " = " + fmt(node.x()), "f(" + name + ") = " + fmt(node.fx()), node.weight()));
        }
        return lines;
    }

    /** Valor numérico del factor: {@code (0.5/2)}, {@code (0.5/3)} o {@code 0.5}. */
    private static String factorText(WeightedRule rule, double dx) {
        String text = rule.factorText().replace("Δx", fmt(dx));
        return text.contains("/") ? "(" + text + ")" : text;
    }

    private static List<GraphPolygon> finiteShapes(List<GraphPolygon> shapes) {
        boolean finite = shapes.stream().flatMap(shape -> shape.vertices().stream())
                .allMatch(v -> Double.isFinite(v.x()) && Double.isFinite(v.y()));
        return finite ? shapes : List.of();
    }

    private static Graph graph(Expression f, double a, double b, List<GraphPolygon> shapes) {
        String label = "Área: ∫[" + FormatUtils.formatShort(a) + ", " + FormatUtils.formatShort(b) + "] f(x) dx";
        GraphArea area = new GraphArea(label, f, Math.min(a, b), Math.max(a, b));
        return new Graph(List.of(curve(f)), List.of(), xRange(a, b), List.of(area), shapes);
    }

    private static GraphCurve curve(Expression f) {
        return new GraphCurve("f(x) = " + f, f, CurveStyle.FUNCTION);
    }

    /** El intervalo [a, b] con un margen a cada lado. */
    private static Range xRange(double a, double b) {
        return Range.centeredAt((a + b) / 2, Math.max(Math.abs(b - a) * 1.6, 2));
    }

    private static String integral(double a, double b) {
        return "∫[" + fmt(a) + ", " + fmt(b) + "] f(x) dx";
    }

    /** Error con 4 cifras: {@code 0.000006784}. */
    private static String error(double approximation, double reference) {
        return FormatUtils.formatShort(Math.abs(approximation - reference));
    }

    /** Número entre paréntesis si es negativo: {@code (-1)}. */
    private static String signed(double value) {
        return value < 0 ? "(" + fmt(value) + ")" : fmt(value);
    }

    private static String fmt(double value) {
        return FormatUtils.formatNumber(value);
    }
}
