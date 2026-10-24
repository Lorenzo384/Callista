package com.callista.model;

import com.callista.model.calculus.area.AreaBetweenCurves;
import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.calculus.integral.DefiniteIntegral;
import com.callista.model.calculus.integral.IndefiniteIntegral;
import com.callista.model.calculus.revolution.RotationAxis;
import com.callista.model.calculus.revolution.SolidOfRevolution;
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
import com.callista.model.expression.Negation;
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
            EnumSet.of(OperationType.EVALUATE, OperationType.DERIVATIVE,
                    OperationType.INDEFINITE_INTEGRAL, OperationType.DEFINITE_INTEGRAL,
                    OperationType.AREA_BETWEEN_CURVES, OperationType.SOLID_OF_REVOLUTION);
    /**
     * Operaciones que aceptan y en la función. Las demás grafican y evalúan
     * y = f(x), donde y no tendría valor.
     */
    private static final Set<OperationType> ACCEPTS_Y =
            EnumSet.of(OperationType.DERIVATIVE, OperationType.INDEFINITE_INTEGRAL,
                    OperationType.DEFINITE_INTEGRAL, OperationType.AREA_BETWEEN_CURVES);
    private static final Variable Y = new Variable("y");
    private static final String[] ORDINALS = {"", "Primera", "Segunda", "Tercera"};

    private final ExpressionParser parser = new ExpressionParser();
    private final DerivativeCalculator derivativeCalculator = new DerivativeCalculator();
    private final DefiniteIntegral definiteIntegral = new DefiniteIntegral();
    private final IndefiniteIntegral indefiniteIntegral = new IndefiniteIntegral();
    private final AreaBetweenCurves areaBetweenCurves = new AreaBetweenCurves();
    private final SolidOfRevolution solidOfRevolution = new SolidOfRevolution();

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
        Expression function = parser.parse(text);
        if (function.contains(Constant.INFINITY)) {
            throw new InvalidExpressionException("∞ solo se puede usar en los límites de una integral", 1);
        }
        return function;
    }

    /**
     * Interpreta una función para la operación indicada: además de que sea
     * válida, revisa que solo use y si la operación la acepta.
     *
     * @throws InvalidExpressionException si el texto no es válido o usa y
     *                                    en una operación que solo trabaja con x
     */
    public Expression interpret(String text, OperationType operation) throws InvalidExpressionException {
        Expression function = interpret(text);
        if (!ACCEPTS_Y.contains(operation) && function.contains(Y)) {
            throw new InvalidExpressionException("Esta operación solo usa x; y se acepta en derivadas, integrales y áreas",
                    text.indexOf('y') + 1);
        }
        return function;
    }

    /**
     * Interpreta un valor numérico. Acepta expresiones sin variable, como
     * {@code 2}, {@code -1.5} o {@code pi/2}.
     *
     * @throws InvalidExpressionException si el texto no es válido o no da un
     *                                    número real (por ejemplo, si contiene x)
     */
    public double interpretValue(String text) throws InvalidExpressionException {
        return interpretConstant(text).evaluate(0);
    }

    /**
     * Interpreta un valor sin evaluarlo, para conservarlo exacto: {@code pi/2}
     * sigue siendo {@code π/2}.
     *
     * @throws InvalidExpressionException si el texto no es válido, contiene x
     *                                    o no da un número real
     */
    public Expression interpretConstant(String text) throws InvalidExpressionException {
        Expression value = parser.parse(text);
        if (value.dependsOnVariable() || !Double.isFinite(value.evaluate(0))) {
            throw new InvalidExpressionException(
                    "El valor debe ser un número real, por ejemplo 2, -1.5 o pi/2", 1);
        }
        return value;
    }

    /**
     * Interpreta el límite de una integral: un número como en
     * {@link #interpretConstant} o infinito ({@code inf}, {@code -inf}, {@code ∞}).
     *
     * @throws InvalidExpressionException si el texto no es válido o no es un
     *                                    número real ni ±∞
     */
    public Expression interpretLimit(String text) throws InvalidExpressionException {
        Expression value = parser.parse(text);
        double number = value.evaluate(0);
        boolean infinite = value.contains(Constant.INFINITY) && Double.isInfinite(number);
        if (value.dependsOnVariable() || !(Double.isFinite(number) || infinite)) {
            throw new InvalidExpressionException(
                    "El límite debe ser un número real (2, -1.5, pi/2) o infinito (inf, -inf)", 1);
        }
        return infinite ? (number > 0 ? Constant.INFINITY : new Negation(Constant.INFINITY)) : value;
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
     * Deriva la función order veces respecto de x.
     */
    public CalculationResult derive(Expression function, int order) {
        return derive(function, order, OptionalDouble.empty(), "x");
    }

    /**
     * Deriva respecto de x y evalúa en un punto.
     */
    public CalculationResult derive(Expression function, int order, double point) {
        return derive(function, order, OptionalDouble.of(point), "x");
    }

    /**
     * Deriva respecto de la variable indicada, sin evaluar en un punto.
     *
     * @param variableName variable respecto de la que se deriva: x o y
     */
    public CalculationResult derive(
            Expression function, int order, String variableName) {
        return derive(function, order, OptionalDouble.empty(), variableName);
    }

    /**
     * Método interno para realizar la derivación.
     */
    private CalculationResult derive(
            Expression function,
            int order,
            OptionalDouble point,
            String variableName) {

        if (order < 1 || order > MAX_DERIVATIVE_ORDER) {
            throw new IllegalArgumentException(
                    "Orden de derivada fuera de rango: " + order);
        }

        if (!"x".equals(variableName) && !"y".equals(variableName)) {
            throw new IllegalArgumentException(
                    "La variable de derivación debe ser x o y.");
        }

        boolean hasX = function.contains(new Variable("x"));
        boolean hasY = function.contains(new Variable("y"));
        // Es parcial si f también depende de la otra variable
        String otherVariable = "x".equals(variableName) ? "y" : "x";
        boolean partial = function.dependsOn(otherVariable);
        // La gráfica es de y = f(x): no aplica si f usa y o se deriva respecto de y
        boolean graphable = "x".equals(variableName) && !hasY;

        if (point.isPresent() && !graphable) {
            throw new IllegalArgumentException(
                    "La evaluación puntual de una derivada parcial requiere "
                            + "valores para x e y.");
        }

        String functionName = hasX && hasY
                ? "f(x,y)"
                : hasY ? "f(y)" : "f(x)";

        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep(
                "Función: " + functionName + " = " + function));

        int ruleLevel = order > 1 ? 1 : 0;
        Expression current = function;

        DerivativeCalculator calculator =
                variableName.equals("x")
                        ? derivativeCalculator
                        : new DerivativeCalculator(variableName);

        for (int k = 1; k <= order; k++) {
            if (order > 1) {
                steps.add(new SolutionStep(
                        ORDINALS[k] + " derivada: se deriva "
                                + derivativeName(
                                k - 1, variableName,
                                partial, functionName)
                                + " = " + current));
            }

            Expression derivative =
                    calculator.derive(current, steps, ruleLevel);

            current = simplify(
                    derivative,
                    derivativeName(
                            k, variableName, partial, functionName),
                    steps,
                    ruleLevel);
        }

        String name = derivativeName(
                order, variableName, partial, functionName);

        String summary = name + " = " + current;

        /*
         * Las expresiones multivariables todavía no se representan
         * en la gráfica bidimensional existente.
         */
        if (!graphable) {
            return new CalculationResult(summary, steps);
        }

        List<GraphCurve> curves = new ArrayList<>(List.of(
                new GraphCurve(
                        functionName + " = " + function,
                        function,
                        CurveStyle.FUNCTION),
                new GraphCurve(
                        name + " = " + current,
                        current,
                        CurveStyle.DERIVATIVE)));

        if (point.isEmpty()) {
            return new CalculationResult(
                    summary,
                    steps,
                    new Graph(curves, List.of(), DEFAULT_X_RANGE));
        }

        double x = point.getAsDouble();
        double value = current.evaluate(x);
        String at = FormatUtils.formatNumber(x);
        String image = name.replace("(x)", "(" + at + ")");

        Range xRange = Range.centeredAt(x, POINT_X_WIDTH);

        if (!Double.isFinite(value)) {
            steps.add(new SolutionStep(
                    image + " no está definida: x = " + at
                            + " está fuera del dominio de la derivada"));

            return new CalculationResult(
                    image + " no está definida",
                    steps,
                    new Graph(curves, List.of(), xRange));
        }

        String result = image + " = " + FormatUtils.formatNumber(value);

        steps.add(new SolutionStep(
                "Evaluar en x = " + at + ": " + result));

        List<GraphPoint> points = new ArrayList<>();
        points.add(new GraphPoint(pointLabel(x, value), x, value));

        double fx = function.evaluate(x);

        if (order == 1 && Double.isFinite(fx)) {
            Expression tangent = tangentLine(x, fx, value, steps);
            curves.add(new GraphCurve(
                    "Tangente: y = " + tangent,
                    tangent,
                    CurveStyle.TANGENT));
            points.add(new GraphPoint(pointLabel(x, fx), x, fx));
        }

        return new CalculationResult(
                result, steps, new Graph(curves, points, xRange));
    }

    public CalculationResult integrate(Expression function) {
        return indefiniteIntegral.solve(function);
    }
    /**
     * Integral indefinida respecto de la variable seleccionada.
     */
    public CalculationResult integrate(
            Expression function, String variableName) {

        return indefiniteIntegral.solve(function, variableName);
    }

    /**
     * Integral definida exacta con el Teorema Fundamental del Cálculo; si es
     * impropia, como límite (converge o diverge).
     *
     * @param a límite inferior, sin evaluar (ver {@link #interpretLimit})
     * @param b límite superior, sin evaluar
     */
    public CalculationResult exactIntegral(Expression function, Expression a, Expression b) {
        return definiteIntegral.exact(function, a, b);
    }
    /**
     * Integral definida exacta respecto de la variable indicada.
     */
    public CalculationResult exactIntegral(
            Expression function,
            Expression a,
            Expression b,
            String variableName) {

        return definiteIntegral.exact(
                function, a, b, variableName);
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

    /**
     * Área de la región encerrada por f y g, del primer al último punto de corte.
     *
     * @throws ParameterException si las curvas no encierran una región
     */
    public CalculationResult enclosedArea(Expression f, Expression g) throws ParameterException {
        return enclosedArea(f, g, "x");
    }

    /** Área encerrada de f y g respecto de x o y. */
    public CalculationResult enclosedArea(Expression f, Expression g, String variableName)
            throws ParameterException {
        return areaBetweenCurves.enclosed(f, g, variableName);
    }

    /**
     * Área entre f y g en [a, b], partiendo el intervalo en los puntos de corte.
     *
     * @param a límite inferior, sin evaluar (ver {@link #interpretConstant})
     * @param b límite superior, sin evaluar
     * @throws ParameterException si a no es menor que b, o si f y g son iguales
     */
    public CalculationResult areaBetween(Expression f, Expression g, Expression a, Expression b)
            throws ParameterException {
        return areaBetween(f, g, a, b, "x");
    }

    /** Área entre f y g en [a, b], respecto de x o y. */
    public CalculationResult areaBetween(Expression f, Expression g, Expression a, Expression b,
                                         String variableName) throws ParameterException {
        return areaBetweenCurves.between(f, g, a, b, variableName);
    }

    /**
     * Volumen al girar alrededor de {@code axis} la región que encierran f y g.
     *
     * @throws ParameterException si no encierran una región o el eje la atraviesa
     */
    public CalculationResult enclosedVolume(Expression f, Expression g, RotationAxis axis) throws ParameterException {
        return solidOfRevolution.enclosed(f, g, axis);
    }

    /**
     * Volumen al girar alrededor de {@code axis} la región entre f y g en [a, b].
     *
     * @throws ParameterException si a no es menor que b, o si el eje vertical atraviesa la región
     */
    public CalculationResult volumeBetween(Expression f, Expression g, Expression a, Expression b, RotationAxis axis)
            throws ParameterException {
        return solidOfRevolution.between(f, g, a, b, axis);
    }

    /**
     * La curva g(x) = 0 (el eje x), para cuando no se escribe g.
     */
    public Expression xAxis() {
        return AreaBetweenCurves.xAxis();
    }

    /**
     * Recta tangente a f en x = a.
     */
    private static Expression tangentLine(
            double a,
            double fa,
            double slope,
            List<SolutionStep> steps) {

        Variable x = new Variable("x");
        Constant m = rounded(slope);

        Expression xMinusA = Simplifier.difference(x, rounded(a));

        Expression substituted = Simplifier.sum(
                rounded(fa),
                Simplifier.product(m, xMinusA));

        Expression line = Simplifier.sum(
                Simplifier.product(m, x),
                rounded(fa - m.getValue() * a));

        String at = FormatUtils.formatNumber(a);

        String factor = xMinusA instanceof Variable
                ? xMinusA.toString()
                : "(" + xMinusA + ")";

        steps.add(new SolutionStep(
                "Recta tangente en x = " + at
                        + ": y = f(" + at + ") + f'(" + at + ")*"
                        + factor + " = " + substituted
                        + (line.equals(substituted) ? "" : " = " + line)));

        return line;
    }

    /**
     * Etiqueta breve de un punto en la gráfica.
     */
    private static String pointLabel(double x, double y) {
        return "("
                + FormatUtils.formatShort(x)
                + ", "
                + FormatUtils.formatShort(y)
                + ")";
    }

    private static Constant rounded(double value) {
        return new Constant(
                Double.parseDouble(FormatUtils.formatNumber(value)));
    }

    /**
     * Simplifica un resultado y registra los pasos correspondientes.
     */
    private static Expression simplify(
            Expression result,
            String name,
            List<SolutionStep> steps,
            int level) {

        Set<SimplificationRule> applied =
                EnumSet.noneOf(SimplificationRule.class);

        Expression simplified = Simplifier.simplify(result, applied);

        if (simplified.equals(result)) {
            steps.add(new SolutionStep(
                    "Resultado: " + name + " = " + result,
                    level));

            return result;
        }

        String how = applied.isEmpty()
                ? ""
                : " (" + joinInSpanish(applied) + ")";

        steps.add(new SolutionStep(
                "Se obtiene: " + name + " = " + result,
                level));

        steps.add(new SolutionStep(
                "Simplificar" + how + ": " + name + " = " + simplified,
                level));

        return simplified;
    }

    /**
     * Une las descripciones de las reglas de simplificación en español.
     */
    private static String joinInSpanish(
            Set<SimplificationRule> rules) {

        List<String> parts = rules.stream()
                .map(SimplificationRule::getDescription)
                .toList();

        if (parts.size() == 1) {
            return parts.getFirst();
        }

        return String.join(
                ", ",
                parts.subList(0, parts.size() - 1))
                + " y "
                + parts.getLast();
    }

    /** {@code f(x)}, {@code f'(x)}, {@code f''(x)}… */
    private static String derivativeName(
            int order,
            String variableName,
            boolean partial,
            String functionName) {

        if (order == 0) {
            return functionName;
        }

        if (!partial) {
            return "f" + "'".repeat(order) + "(" + variableName + ")";
        }

        if (order == 1) {
            return "∂f/∂" + variableName;
        }

        String superscript = order == 2 ? "²" : "³";

        return "∂" + superscript + "f/∂"
                + variableName + superscript;
    }
}