package com.callista.model;

import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.calculus.integral.DefiniteIntegral;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.GraphPoint;
import com.callista.model.entity.OperationType;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;
import com.callista.model.expression.SimplificationRule;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Variable;
import com.callista.model.graph.Range;
import com.callista.model.parser.ExpressionParser;
import com.callista.util.FormatUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Fachada del modelo: único punto de entrada que usan los controladores.
 * No conoce nada de la interfaz gráfica.
 */
public class CalculatorModel {

    /** Intervalo de x que se grafica cuando no hay un punto de interés. */
    private static final Range DEFAULT_X_RANGE = new Range(-10, 10);
    /** Ancho del intervalo de x alrededor de un punto de interés. */
    private static final double POINT_X_WIDTH = 12;

    /** Orden máximo de derivada que se puede pedir. */
    public static final int MAX_DERIVATIVE_ORDER = 3;

    private static final Set<OperationType> SUPPORTED =
            EnumSet.of(OperationType.EVALUATE, OperationType.DERIVATIVE, OperationType.DEFINITE_INTEGRAL);
    private static final String[] ORDINALS = {"", "Primera", "Segunda", "Tercera"};

    private final ExpressionParser parser = new ExpressionParser();
    private final DerivativeCalculator derivativeCalculator = new DerivativeCalculator();
    private final DefiniteIntegral definiteIntegral = new DefiniteIntegral();

    /**
     * Operaciones ya implementadas; el resto se muestran como «próximamente».
     */
    public Set<OperationType> getSupportedOperations() {
        return SUPPORTED;
    }

    /**
     * Interpreta la función escrita por el usuario.
     *
     * @throws InvalidExpressionException si el texto no es una expresión válida
     */
    public Expression interpret(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    /**
     * Interpreta un valor numérico. Acepta expresiones sin variable, como
     * {@code 2}, {@code -1.5} o {@code pi/2}.
     *
     * @throws InvalidExpressionException si el texto no es válido o no da un
     *                                    número real (por ejemplo, si contiene x)
     */
    public double interpretValue(String text) throws InvalidExpressionException {
        // Se evalúa con x = NaN: si el valor depende de x, el resultado será NaN.
        double value = parser.parse(text).evaluate(Double.NaN);
        if (!Double.isFinite(value)) {
            throw new InvalidExpressionException(
                    "El valor debe ser un número real, por ejemplo 2, -1.5 o pi/2", 1);
        }
        return value;
    }

    /**
     * Evalúa la función en un punto y explica el procedimiento.
     */
    public CalculationResult evaluate(Expression function, double x) {
        double y = function.evaluate(x);
        String point = FormatUtils.formatNumber(x);
        String image = "f(" + point + ")";
        boolean defined = Double.isFinite(y);

        String conclusion = defined
                ? "Resultado: " + image + " = " + FormatUtils.formatNumber(y)
                : image + " no está definida: x = " + point + " está fuera del dominio de la función";
        List<SolutionStep> steps = List.of(
                new SolutionStep("Función: f(x) = " + function),
                new SolutionStep("Sustituir x = " + point + " (los ángulos se miden en radianes)"),
                new SolutionStep(conclusion));

        String summary = defined ? image + " = " + FormatUtils.formatNumber(y) : image + " no está definida";
        List<GraphPoint> points = defined
                ? List.of(new GraphPoint(pointLabel(x, y), x, y))
                : List.of();
        Graph graph = new Graph(List.of(new GraphCurve("f(x) = " + function, function, CurveStyle.FUNCTION)),
                points, Range.centeredAt(x, POINT_X_WIDTH));
        return new CalculationResult(summary, steps, graph);
    }

    /**
     * Deriva la función {@code order} veces y explica cada regla aplicada.
     *
     * @param order orden de la derivada, de 1 a {@value #MAX_DERIVATIVE_ORDER}
     */
    public CalculationResult derive(Expression function, int order) {
        return derive(function, order, OptionalDouble.empty());
    }

    /**
     * Deriva la función {@code order} veces y evalúa la derivada en {@code point}.
     *
     * @param order orden de la derivada, de 1 a {@value #MAX_DERIVATIVE_ORDER}
     */
    public CalculationResult derive(Expression function, int order, double point) {
        return derive(function, order, OptionalDouble.of(point));
    }

    private CalculationResult derive(Expression function, int order, OptionalDouble point) {
        if (order < 1 || order > MAX_DERIVATIVE_ORDER) {
            throw new IllegalArgumentException("Orden de derivada fuera de rango: " + order);
        }
        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep("Función: f(x) = " + function));

        // Con varias derivadas, cada una es un paso principal y sus reglas van un nivel adentro
        int ruleLevel = order > 1 ? 1 : 0;
        Expression current = function;
        for (int k = 1; k <= order; k++) {
            if (order > 1) {
                steps.add(new SolutionStep(ORDINALS[k] + " derivada: se deriva "
                        + derivativeName(k - 1) + " = " + current));
            }
            Expression derivative = derivativeCalculator.derive(current, steps, ruleLevel);
            current = simplify(derivative, derivativeName(k), steps, ruleLevel);
        }

        String name = derivativeName(order);
        List<GraphCurve> curves = new ArrayList<>(List.of(
                new GraphCurve("f(x) = " + function, function, CurveStyle.FUNCTION),
                new GraphCurve(name + " = " + current, current, CurveStyle.DERIVATIVE)));
        if (point.isEmpty()) {
            return new CalculationResult(name + " = " + current, steps, new Graph(curves, List.of(), DEFAULT_X_RANGE));
        }

        double x = point.getAsDouble();
        double value = current.evaluate(x);
        String at = FormatUtils.formatNumber(x);
        String image = name.replace("(x)", "(" + at + ")");
        Range xRange = Range.centeredAt(x, POINT_X_WIDTH);
        if (!Double.isFinite(value)) {
            steps.add(new SolutionStep(image + " no está definida: x = " + at
                    + " está fuera del dominio de la derivada"));
            return new CalculationResult(image + " no está definida", steps, new Graph(curves, List.of(), xRange));
        }
        String result = image + " = " + FormatUtils.formatNumber(value);
        steps.add(new SolutionStep("Evaluar en x = " + at + ": " + result));

        List<GraphPoint> points = new ArrayList<>();
        points.add(new GraphPoint(pointLabel(x, value), x, value));
        double fx = function.evaluate(x);
        if (order == 1 && Double.isFinite(fx)) {
            Expression tangent = tangentLine(x, fx, value, steps);
            curves.add(new GraphCurve("Tangente: y = " + tangent, tangent, CurveStyle.TANGENT));
            points.add(new GraphPoint(pointLabel(x, fx), x, fx));
        }
        return new CalculationResult(result, steps, new Graph(curves, points, xRange));
    }

    /**
     * Recta tangente a f en x = a, escrita como {@code y = m*x + b} con
     * {@code m = f'(a)} y {@code b = f(a) - m*a}. Agrega el paso que la
     * explica. Los valores se redondean como se muestran
     * ({@value FormatUtils#SIGNIFICANT_DIGITS} cifras) para que la ecuación
     * sea legible.
     */
    private static Expression tangentLine(double a, double fa, double slope, List<SolutionStep> steps) {
        Variable x = new Variable("x");
        Constant m = rounded(slope);
        Expression xMinusA = Simplifier.difference(x, rounded(a));
        Expression substituted = Simplifier.sum(rounded(fa), Simplifier.product(m, xMinusA));
        Expression line = Simplifier.sum(Simplifier.product(m, x), rounded(fa - m.getValue() * a));
        String at = FormatUtils.formatNumber(a);
        String factor = xMinusA instanceof Variable ? xMinusA.toString() : "(" + xMinusA + ")";
        steps.add(new SolutionStep("Recta tangente en x = " + at + ": y = f(" + at + ") + f'(" + at + ")*"
                + factor + " = " + substituted + (line.equals(substituted) ? "" : " = " + line)));
        return line;
    }

    /** Etiqueta breve de un punto en la gráfica: {@code (1.047, 0.866)}. */
    private static String pointLabel(double x, double y) {
        return "(" + FormatUtils.formatShort(x) + ", " + FormatUtils.formatShort(y) + ")";
    }

    private static Constant rounded(double value) {
        return new Constant(Double.parseDouble(FormatUtils.formatNumber(value)));
    }

    /**
     * Simplifica un resultado. Si cambia, lo explica en dos pasos: lo que se
     * obtuvo al aplicar las reglas y la versión simplificada, indicando qué se
     * hizo («agrupar términos semejantes y cancelar factores comunes»).
     */
    private static Expression simplify(Expression result, String name, List<SolutionStep> steps, int level) {
        Set<SimplificationRule> applied = EnumSet.noneOf(SimplificationRule.class);
        Expression simplified = Simplifier.simplify(result, applied);
        if (simplified.equals(result)) {
            steps.add(new SolutionStep("Resultado: " + name + " = " + result, level));
            return result;
        }
        String how = applied.isEmpty() ? "" : " (" + joinInSpanish(applied) + ")";
        steps.add(new SolutionStep("Se obtiene: " + name + " = " + result, level));
        steps.add(new SolutionStep("Simplificar" + how + ": " + name + " = " + simplified, level));
        return simplified;
    }

    /** «a», «a y b», «a, b y c». */
    private static String joinInSpanish(Set<SimplificationRule> rules) {
        List<String> parts = rules.stream().map(SimplificationRule::getDescription).toList();
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " y " + parts.getLast();
    }

    /**
     * Aproxima {@code ∫[a, b] f(x) dx} con un método numérico y explica cada paso.
     *
     * @param n número de subintervalos
     * @throws ParameterException si {@code n} no es válido (por ejemplo, impar en Simpson)
     */
    public CalculationResult approximateIntegral(Expression function, double a, double b,
                                                 NumericalMethod method, int n) throws ParameterException {
        return definiteIntegral.approximate(function, a, b, method, n);
    }

    /**
     * Aproxima {@code ∫[a, b] f(x) dx} con los tres métodos y los compara.
     *
     * @throws ParameterException si {@code n} no es válido
     */
    public CalculationResult compareIntegrationMethods(Expression function, double a, double b, int n)
            throws ParameterException {
        return definiteIntegral.compare(function, a, b, n);
    }

    /** {@code f(x)}, {@code f'(x)}, {@code f''(x)}… */
    private static String derivativeName(int order) {
        return "f" + "'".repeat(order) + "(x)";
    }
}
