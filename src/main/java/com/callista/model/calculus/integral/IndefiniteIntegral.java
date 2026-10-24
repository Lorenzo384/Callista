package com.callista.model.calculus.integral;

import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.calculus.integral.technique.AlgebraicExpansion;
import com.callista.model.calculus.integral.technique.DirectIntegration;
import com.callista.model.calculus.integral.technique.IntegrationByParts;
import com.callista.model.calculus.integral.technique.IntegrationContext;
import com.callista.model.calculus.integral.technique.IntegrationRule;
import com.callista.model.calculus.integral.technique.IntegrationTechnique;
import com.callista.model.calculus.integral.technique.SubstitutionMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Monomial;
import com.callista.model.expression.Product;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Variable;
import com.callista.model.graph.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.callista.model.expression.Simplifier.difference;
import static com.callista.model.expression.Simplifier.product;
import static com.callista.model.expression.Simplifier.sum;

/**
 * Integral indefinida {@code ∫f(x) dx = F(x) + C}.
 *
 * <p>Primero aplica lo que vale para cualquier integral: linealidad (separar
 * sumas y restas) y múltiplo constante (sacar las constantes). Después prueba
 * las técnicas en orden: integrales inmediatas, sustitución, por partes y
 * desarrollar el producto. Cada técnica que no funciona deshace sus pasos y se prueba la
 * siguiente.</p>
 *
 * <p>El resultado se comprueba derivándolo con {@link DerivativeCalculator}.</p>
 */
public class IndefiniteIntegral {

    /** Profundidad máxima de integrales anidadas (sustituciones dentro de sustituciones). */
    private static final int MAX_DEPTH = 8;
    /** Puntos donde se compara la derivada del resultado con la función. */
    private static final double[] CHECK_POINTS = {0.37, 0.81, 1.29, 1.93, 2.48, -0.53, -1.71};

    private final List<IntegrationTechnique> techniques = List.of(
            new DirectIntegration(), new SubstitutionMethod(), new IntegrationByParts(), new AlgebraicExpansion());
    private final DerivativeCalculator derivatives = new DerivativeCalculator();

    /**
     * Antiderivada sin pasos.
     *
     * @throws UnsupportedIntegralException si ninguna técnica funciona
     */


    /**
     * Antiderivada respecto de x, sin pasos.
     */
    public Expression antiderivative(Expression f)
            throws UnsupportedIntegralException {
        return antiderivative(f, new ArrayList<>(), 0, "x");
    }

    /**
     * Antiderivada respecto de la variable indicada, sin pasos.
     */
    public Expression antiderivative(Expression f, String variableName)
            throws UnsupportedIntegralException {
        return antiderivative(f, new ArrayList<>(), 0, variableName);
    }

    /**
     * Antiderivada respecto de x, registrando los pasos.
     */
    public Expression antiderivative(
            Expression f, List<SolutionStep> steps, int baseLevel)
            throws UnsupportedIntegralException {
        return antiderivative(f, steps, baseLevel, "x");
    }

    /**
     * Antiderivada respecto de la variable indicada, registrando los pasos.
     */
    public Expression antiderivative(
            Expression f,
            List<SolutionStep> steps,
            int baseLevel,
            String variableName) throws UnsupportedIntegralException {

        if (!"x".equals(variableName) && !"y".equals(variableName)) {
            throw new IllegalArgumentException(
                    "La variable de integración debe ser x o y.");
        }

        return Simplifier.simplify(
                new Integration(steps, variableName, 0)
                        .integrateHere(f, baseLevel));
    }

    /**
     * Resuelve {@code ∫f(x) dx} con procedimiento, comprobación y gráfica. Si
     * no encuentra antiderivada, lo explica en el resultado.
     */
    public CalculationResult solve(Expression f) {
        return solve(f, "x");
    }

    /**
     * Resuelve una integral indefinida respecto de la variable indicada.
     */
    public CalculationResult solve(Expression f, String variableName) {
        if (!"x".equals(variableName) && !"y".equals(variableName)) {
            throw new IllegalArgumentException(
                    "La variable de integración debe ser x o y.");
        }

        String functionName = functionLabel(f, variableName);
        String integralName = "∫" + functionName + " d" + variableName;

        boolean graphable =
                "x".equals(variableName) && !f.dependsOn("y");

        List<SolutionStep> steps = new ArrayList<>();
        steps.add(new SolutionStep("Función: " + functionName + " = " + f));

        Expression antiderivative;

        try {
            antiderivative = antiderivative(f, steps, 0, variableName);
        } catch (UnsupportedIntegralException e) {
            steps.add(new SolutionStep(e.getMessage()));
            steps.add(new SolutionStep(
                    "Si necesitas un valor, usa el modo «Integral definida» "
                            + "con un método numérico: no requiere antiderivada."));

            if (!graphable) {
                return new CalculationResult(
                        integralName + ": no se encontró antiderivada",
                        steps);
            }

            Graph graph = new Graph(
                    List.of(new GraphCurve(
                            "f(x) = " + f, f, CurveStyle.FUNCTION)),
                    List.of(),
                    new Range(-10, 10));

            return new CalculationResult(
                    integralName + ": no se encontró antiderivada",
                    steps,
                    graph);
        }

        steps.add(new SolutionStep(
                "Resultado: " + integralName + " = "
                        + antiderivative + " + C"));

        steps.add(new SolutionStep(
                verification(f, antiderivative, variableName)));

        String summary = integralName + " = " + antiderivative + " + C";

        /*
         * No dibujamos expresiones multivariables en la gráfica
         * bidimensional actual.
         */
        if (!graphable) {
            return new CalculationResult(summary, steps);
        }

        Graph graph = new Graph(
                List.of(
                        new GraphCurve(
                                "f(x) = " + f,
                                f,
                                CurveStyle.FUNCTION),
                        new GraphCurve(
                                "F(x) = " + antiderivative + "  (C = 0)",
                                antiderivative,
                                CurveStyle.ANTIDERIVATIVE)),
                List.of(),
                new Range(-10, 10));

        return new CalculationResult(summary, steps, graph);
    }



    /** Describe la función según las variables que contiene. */
    private static String functionLabel(
            Expression f, String variableName) {
        boolean hasX = f.dependsOn("x");
        boolean hasY = f.dependsOn("y");

        if (hasX && hasY) {
            return "f(x,y)";
        }
        if (hasY) {
            return "f(y)";
        }
        if (hasX) {
            return "f(x)";
        }

        return "f(" + variableName + ")";
    }

    /**
     * Comprueba el resultado derivándolo respecto de la variable de integración.
     * La comparación numérica mantiene constante la otra variable.
     */
    private String verification(
            Expression f,
            Expression antiderivative,
            String variableName) {

        DerivativeCalculator calculator = "x".equals(variableName)
                ? derivatives
                : new DerivativeCalculator(variableName);

        Expression derivative = Simplifier.simplify(
                calculator.derive(antiderivative));

        boolean partial = "y".equals(variableName) || f.dependsOn("y");
        String notation = partial
                ? "∂/∂" + variableName
                : "d/d" + variableName;
        String functionName = functionLabel(f, variableName);

        return "Comprobación: " + notation + "["
                + antiderivative + "] = " + derivative
                + (sameFunction(f, derivative, variableName)
                ? " = " + functionName + " ✓"
                : " ≠ " + functionName + " ✗");
    }

    /**
     * Indica si dos funciones valen lo mismo en los puntos de prueba donde
     * ambas están definidas (y en al menos uno).
     */
    static boolean sameFunction(Expression f, Expression g) {
        int compared = 0;
        for (double x : CHECK_POINTS) {
            double a = f.evaluate(x);
            double b = g.evaluate(x);
            if (Double.isFinite(a) && Double.isFinite(b)) {
                if (Math.abs(a - b) > 1e-7 * Math.max(1, Math.abs(a))) {
                    return false;
                }
                compared++;
            }
        }
        return compared > 0;
    }
    static boolean sameFunction(
            Expression f,
            Expression g,
            String variableName) {

        int compared = 0;
        double parameter = 0.731;

        for (double t : CHECK_POINTS) {
            double x = "x".equals(variableName) ? t : parameter;
            double y = "y".equals(variableName) ? t : parameter;

            double a = f.evaluate(x, y);
            double b = g.evaluate(x, y);

            if (Double.isFinite(a) && Double.isFinite(b)) {
                if (Math.abs(a - b)
                        > 1e-7 * Math.max(1, Math.abs(a))) {
                    return false;
                }
                compared++;
            }
        }

        return compared > 0;
    }


    /**
     * Una integración en curso: la lista de pasos que se llena, la variable y
     * la profundidad de anidamiento.
     */
    private final class Integration implements IntegrationContext {

        private final List<SolutionStep> steps;
        private final String variable;
        private final int depth;

        private Integration(List<SolutionStep> steps, String variable, int depth) {
            this.steps = steps;
            this.variable = variable;
            this.depth = depth;
        }

        @Override
        public String variableName() {
            return variable;
        }

        /**
         * Una integral anidada (cada término de una suma, la v de partes, la
         * integral en u…): cuenta un nivel más de profundidad, para que una
         * técnica que genera integrales cada vez peores termine.
         */
        @Override
        public Expression integrate(Expression integrand, int level) throws UnsupportedIntegralException {
            return new Integration(steps, variable, depth + 1).integrateHere(integrand, level);
        }

        private Expression integrateHere(Expression integrand, int level) throws UnsupportedIntegralException {
            if (depth > MAX_DEPTH) {
                throw unsupported(integrand);
            }
            Expression f = Simplifier.simplify(integrand);
            Variable x = new Variable(variable);

            if (!f.dependsOn(variable)) {
                Expression result = product(f, x);
                record(level, IntegrationRule.CONSTANT,
                        integral(f) + " = " + result);
                return result;
            }

            List<Monomial> terms = Simplifier.terms(f);
            if (terms.size() > 1) {
                return integrateTerms(f, terms, level);
            }

            Monomial term = terms.getFirst();
            Monomial constant = term.constantPart(variable);
            Monomial variablePart = term.variablePart(variable);
            boolean hasConstant = !constant.coefficient().isOne() || !constant.factors().isEmpty();
            if (hasConstant) {
                Expression k = constant.toExpression();
                Expression rest = variablePart.toExpression();
                record(level, IntegrationRule.CONSTANT_MULTIPLE,
                        integral(f) + " = " + factor(k) + "*" + integral(rest));
                return product(k, integrateVariablePart(variablePart, level + 1));
            }
            return integrateVariablePart(variablePart, level);
        }

        @Override
        public Expression integrate(Expression integrand, String otherVariable, int level)
                throws UnsupportedIntegralException {
            return new Integration(steps, otherVariable, depth + 1).integrate(integrand, level);
        }

        /**
         * Linealidad: la integral de una suma es la suma de las integrales.
         * Los términos negativos se integran sin el signo y se restan.
         */
        private Expression integrateTerms(Expression f, List<Monomial> terms, int level)
                throws UnsupportedIntegralException {
            StringBuilder statement = new StringBuilder(integral(f)).append(" = ");
            for (int i = 0; i < terms.size(); i++) {
                Monomial term = terms.get(i);
                boolean negative = term.coefficient().signum() < 0;
                if (i > 0) {
                    statement.append(negative ? " - " : " + ");
                } else if (negative) {
                    statement.append("-");
                }
                statement.append(integral(positive(term)));
            }
            record(level, IntegrationRule.LINEARITY, statement.toString());

            Expression result = null;
            for (Monomial term : terms) {
                Expression integralOfTerm = integrate(positive(term), level + 1);
                boolean negative = term.coefficient().signum() < 0;
                if (result == null) {
                    result = negative ? Simplifier.negate(integralOfTerm) : integralOfTerm;
                } else {
                    result = negative ? difference(result, integralOfTerm) : sum(result, integralOfTerm);
                }
            }
            return result;
        }

        private Expression integrateVariablePart(Monomial integrand, int level) throws UnsupportedIntegralException {
            for (IntegrationTechnique technique : techniques) {
                int checkpoint = checkpoint();
                try {
                    Optional<Expression> result = technique.integrate(integrand, this, level);
                    if (result.isPresent()) {
                        return result.get();
                    }
                } catch (UnsupportedIntegralException tryNext) {
                    // La técnica aplicaba pero algo intermedio no se pudo integrar
                }
                rollback(checkpoint);
            }
            throw unsupported(integrand.toExpression());
        }

        private UnsupportedIntegralException unsupported(Expression f) {
            return new UnsupportedIntegralException("No se encontró una antiderivada de " + f
                    + " con las técnicas disponibles (integrales inmediatas, sustitución, por partes y desarrollar el producto). "
                    + "Puede que no tenga antiderivada elemental, como e^(-x^2) o sin(x)/x.");
        }

        @Override
        public void record(int level, String title, String statement) {
            steps.add(new SolutionStep(title + ": " + statement, level));
        }

        @Override
        public int checkpoint() {
            return steps.size();
        }

        @Override
        public void rollback(int checkpoint) {
            steps.subList(checkpoint, steps.size()).clear();
        }
    }

    private static Expression positive(Monomial term) {
        return term.coefficient().signum() < 0
                ? new Monomial(term.coefficient().negate(), term.factors()).toExpression()
                : term.toExpression();
    }

    /** El factor constante con paréntesis si es suma o fracción: {@code (1/2)*∫cos(u) du}. */
    private static String factor(Expression e) {
        return e.precedence() <= Expression.MULTIPLICATIVE && !(e instanceof Product)
                ? "(" + e + ")" : e.toString();
    }
}