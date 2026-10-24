package com.callista.model.calculus.integral;

import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.GraphCurve;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndefiniteIntegralTest {

    private final ExpressionParser parser = new ExpressionParser();
    private final IndefiniteIntegral integral = new IndefiniteIntegral();

    private Expression f(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    private String antiderivative(String text) throws InvalidExpressionException, UnsupportedIntegralException {
        return integral.antiderivative(f(text)).toString();
    }

    private List<String> steps(String text) throws InvalidExpressionException, UnsupportedIntegralException {
        List<SolutionStep> steps = new ArrayList<>();
        integral.antiderivative(f(text), steps, 0);
        return steps.stream().map(SolutionStep::getDescription).toList();
    }

    /** Las antiderivadas se escriben como en un libro. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // inmediatas
            "5                    | 5*x",
            "3x^2 + 2x - 5        | x^3 + x^2 - 5*x",
            "x^-2                 | -1/x",
            "1/x                  | ln(abs(x))",
            "sqrt(x)              | 2*x^(3/2)/3",
            "sen(x) + cos(x)      | sin(x) - cos(x)",
            "e^x                  | e^x",
            "2^x                  | 2^x/ln(2)",
            "sec(x)^2             | tan(x)",
            "tan(x)               | -ln(abs(cos(x)))",
            "1/(1 + x^2)          | arctan(x)",
            "1/sqrt(1 - x^2)      | arcsin(x)",
            "cos(x)/sen(x)^2      | -csc(x)",
            "pi*x                 | π*x^2/2",
            // sustitución
            "sen(3x)              | -cos(3*x)/3",
            "e^(2x)               | e^(2*x)/2",
            "1/(3x + 1)           | ln(abs(3*x + 1))/3",
            "x*cos(x^2)           | sin(x^2)/2",
            "2x/(x^2 + 1)         | ln(abs(x^2 + 1))",
            "x*(x^2 + 1)^5        | (x^2 + 1)^6/12",
            "ln(x)/x              | ln(x)^2/2",
            "sen(x)^3*cos(x)      | sin(x)^4/4",
            "x/sqrt(x^2 + 1)      | sqrt(x^2 + 1)",
            "x*e^(x^2)            | e^(x^2)/2",
            "(x + 1)^2            | (x + 1)^3/3",
            // desarrollar el producto
            "x*(x + 1)            | x^3/3 + x^2/2",
            "(x^2 + 1)/x          | x^2/2 + ln(abs(x))",
    })
    void integratesInTextbookForm(String function, String expected)
            throws InvalidExpressionException, UnsupportedIntegralException {
        assertEquals(expected, antiderivative(function));
    }
    @Test
    void integratesMultivariableExpressionWithRespectToXAndY()
            throws InvalidExpressionException, UnsupportedIntegralException {

        Expression function = f("x^3 + 3y");

        Expression integralX = integral.antiderivative(function, "x");
        Expression integralY = integral.antiderivative(function, "y");

        // ∫(x^3 + 3y) dx = x^4/4 + 3xy
        assertEquals(28.0, integralX.evaluate(2.0, 4.0), 1e-9);

        // ∫(x^3 + 3y) dy = x^3*y + 3*y^2/2
        assertEquals(56.0, integralY.evaluate(2.0, 4.0), 1e-9);

        double h = 1e-5;

        double derivativeX =
                (integralX.evaluate(2.0 + h, 4.0)
                        - integralX.evaluate(2.0 - h, 4.0))
                        / (2.0 * h);

        double derivativeY =
                (integralY.evaluate(2.0, 4.0 + h)
                        - integralY.evaluate(2.0, 4.0 - h))
                        / (2.0 * h);

        assertEquals(20.0, derivativeX, 1e-4);
        assertEquals(20.0, derivativeY, 1e-4);
    }

    /**
     * Comprueba la matemática: la derivada numérica de F coincide con f en
     * varios puntos.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "3x^2 + 2x - 5", "x^-3 + x^(2/3)", "4/x - 1/x^2", "e^x + 2^x", "sen(x) - 2cos(x)", "sec(x)^2 + csc(x)^2",
            "sec(x)*tan(x)", "tan(x) + cot(x)", "sec(x)", "csc(x)", "senh(x) + cosh(x)", "tanh(x)", "5/(1 + x^2)",
            "sen(5x + 1)", "cos(x/2)", "e^(-3x)", "x^2*e^(x^3)", "x*sqrt(x^2 + 4)", "(2x + 3)/(x^2 + 3x + 5)",
            "cos(x)*e^(sen(x))", "sec(x)^2*tan(x)^3", "1/(x*ln(x))", "e^x/(1 + e^(2x))", "x^3*(x^4 + 2)^(-2)",
            "(3x - 1)^7", "x*(2x - 1)^2", "(x^3 + x)/x^2", "sen(x)*cos(x)^4", "1/sqrt(x)*cos(sqrt(x))",
    })
    void derivativeOfTheResultIsTheFunction(String text) throws InvalidExpressionException, UnsupportedIntegralException {
        Expression function = f(text);
        Expression antiderivative = integral.antiderivative(function);
        double h = 1e-6;
        int compared = 0;
        for (double x : new double[]{0.3, 0.7, 1.1, 1.6, 2.2}) {
            double expected = function.evaluate(x);
            double numeric = (antiderivative.evaluate(x + h) - antiderivative.evaluate(x - h)) / (2 * h);
            if (Double.isFinite(expected) && Double.isFinite(numeric)) {
                assertEquals(expected, numeric, 1e-4 * Math.max(1, Math.abs(expected)),
                        () -> text + " en x = " + x + ": F = " + antiderivative);
                compared++;
            }
        }
        assertTrue(compared >= 3, text + ": muy pocos puntos comparados");
    }

    /** Sin antiderivada elemental, o requieren técnicas fuera del temario. */
    @ParameterizedTest
    @ValueSource(strings = {"e^(-x^2)", "sen(x)/x", "sen(x^2)", "1/ln(x)", "e^x/x", "x*arctan(x)"})
    void reportsWhatItCannotIntegrate(String text) {
        UnsupportedIntegralException error = assertThrows(UnsupportedIntegralException.class,
                () -> integral.antiderivative(f(text)));
        assertTrue(error.getMessage().startsWith("No se encontró una antiderivada"));
    }

    @Test
    void explainsTheSubstitution() throws InvalidExpressionException, UnsupportedIntegralException {
        assertEquals(List.of(
                "Sustitución: ∫x*cos(x^2) dx: u = x^2,  du = 2*x dx",
                "La integral queda: ∫cos(u)/2 du",
                "Múltiplo constante: ∫cos(u)/2 du = (1/2)*∫cos(u) du",
                "Integral del coseno: ∫cos(u) du = sin(u)",
                "Regresar a la variable original: u = x^2: ∫x*cos(x^2) dx = sin(x^2)/2"),
                steps("x*cos(x^2)"));
    }

    @Test
    void explainsLinearity() throws InvalidExpressionException, UnsupportedIntegralException {
        List<String> steps = steps("3x^2 - 2/x");
        assertEquals("Linealidad: ∫(3*x^2 - 2/x) dx = ∫3*x^2 dx - ∫2/x dx", steps.getFirst());
        assertTrue(steps.contains("Regla de la potencia: ∫x^2 dx = x^3/3"));
        assertTrue(steps.contains("Integral de 1/x: ∫1/x dx = ln(abs(x))"));
    }

    @Test
    void discardsTheStepsOfTechniquesThatDidNotWork() throws InvalidExpressionException, UnsupportedIntegralException {
        // Primero se intenta la sustitución con u = x^2 + 1 y falla; solo queda el desarrollo
        List<String> steps = steps("(x^2 + 1)/x");
        assertTrue(steps.stream().noneMatch(s -> s.startsWith("Sustitución")), steps::toString);
        assertTrue(steps.getFirst().startsWith("Desarrollar el producto"));
    }

    @Test
    void solveAddsTheConstantAndChecksByDifferentiating() throws InvalidExpressionException {
        CalculationResult result = integral.solve(f("x*cos(x^2)"));
        assertEquals("∫f(x) dx = sin(x^2)/2 + C", result.getSummary());
        assertEquals("Comprobación: d/dx[sin(x^2)/2] = x*cos(x^2) = f(x) ✓",
                result.getSteps().getLast().getDescription());

        List<GraphCurve> curves = result.getGraph().orElseThrow().curves();
        assertEquals(CurveStyle.ANTIDERIVATIVE, curves.get(1).style());
        assertEquals("F(x) = sin(x^2)/2  (C = 0)", curves.get(1).label());
    }

    @Test
    void solveExplainsWhenThereIsNoAntiderivative() throws InvalidExpressionException {
        CalculationResult result = integral.solve(f("e^(-x^2)"));
        assertEquals("∫f(x) dx: no se encontró antiderivada", result.getSummary());
        assertTrue(result.getSteps().getLast().getDescription().contains("Integral definida"));
    }

    @Test
    void comparesFunctionsNumerically() throws InvalidExpressionException {
        assertTrue(IndefiniteIntegral.sameFunction(f("2/x"), f("2*x/abs(x)^2")));
        assertTrue(!IndefiniteIntegral.sameFunction(f("x"), f("x + 1")));
    }
}
