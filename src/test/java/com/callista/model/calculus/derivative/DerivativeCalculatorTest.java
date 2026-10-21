package com.callista.model.calculus.derivative;

import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivativeCalculatorTest {

    private final ExpressionParser parser = new ExpressionParser();
    private final DerivativeCalculator calculator = new DerivativeCalculator();

    private Expression derive(String text) throws InvalidExpressionException {
        return calculator.derive(parser.parse(text));
    }

    /** La derivada se escribe como en un libro, sin términos sobrantes. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "5                    | 0",
            "x                    | 1",
            "3x^2 + sen(x)        | 6*x + cos(x)",
            "x^3 - 4x + 7         | 3*x^2 - 4",
            "-x^2                 | -2*x",
            "pi*x^2               | 2*π*x",
            "x^-2                 | -2*x^(-3)",
            "x*sen(x)             | sin(x) + x*cos(x)",
            "sen(x)/x             | (cos(x)*x - sin(x))/x^2",
            "1/x                  | -1/x^2",
            "sen(x^2)             | 2*x*cos(x^2)",
            "(x^2+1)^3            | 6*x*(x^2 + 1)^2",
            "e^x                  | e^x",
            "e^(2x)               | 2*e^(2*x)",
            "2^x                  | 2^x*ln(2)",
            "ln(x^2+1)            | 2*x/(x^2 + 1)",
            "raiz(x)              | 1/(2*sqrt(x))",
            "tan(x)               | sec(x)^2",
            "cos(x)               | -sin(x)",
            "x^3 - 4x + ln(x)     | 3*x^2 - 4 + 1/x",
    })
    void derivesInTextbookForm(String function, String expected) throws InvalidExpressionException {
        assertEquals(expected, derive(function).toString());
    }

    /**
     * Comprueba la matemática: la derivada simbólica coincide con la
     * aproximación numérica (f(x + h) - f(x - h)) / 2h en varios puntos.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "3x^2 + sen(x)", "x^5 - 2x^3 + x - 9", "x*sen(x)", "sen(x)/x", "x^2*e^x*cos(x)",
            "(x^2+1)^3", "raiz(x^2 + 1)", "1/(x^2 + 1)", "ln(x)", "log(x)", "ln(x^2 + 1)",
            "e^(-x^2)", "2^x", "3^(x^2)", "x^x", "x^sen(x)", "x^pi", "x^-2", "x^(1/3)",
            "sen(x)", "cos(x)", "tan(x)", "cot(x)", "sec(x)", "csc(x)",
            "arcsen(x/2)", "arccos(x/2)", "arctan(3x)", "senh(x)", "cosh(x)", "tanh(x)",
            "exp(2x)", "abs(x - 5)", "sen(cos(tan(x)))", "-(x^3) + 4/x", "e^x*ln(x)/x^2",
    })
    void matchesTheNumericalDerivative(String text) throws InvalidExpressionException {
        Expression function = parser.parse(text);
        Expression derivative = calculator.derive(function);
        double h = 1e-6;
        for (double x : new double[]{0.3, 0.9, 1.4, 1.8}) {
            double numeric = (function.evaluate(x + h) - function.evaluate(x - h)) / (2 * h);
            double symbolic = derivative.evaluate(x);
            assertEquals(numeric, symbolic, 1e-4 * Math.max(1, Math.abs(numeric)),
                    () -> text + " en x = " + x + ": " + derivative);
        }
    }

    @Test
    void explainsEachRuleWithIndentation() throws InvalidExpressionException {
        List<SolutionStep> steps = new ArrayList<>();
        calculator.derive(parser.parse("3x^2 + sen(x)"), steps, 0);

        assertEquals(List.of(
                "Regla de la suma: d/dx[3*x^2 + sin(x)] = d/dx[3*x^2] + d/dx[sin(x)]",
                "Regla del múltiplo constante: d/dx[3*x^2] = 3*d/dx[x^2]",
                "Regla de la potencia: d/dx[x^2] = 2*x",
                "Derivada del seno: d/dx[sin(x)] = cos(x)"),
                steps.stream().map(SolutionStep::getDescription).toList());
        assertEquals(List.of(0, 1, 2, 1), steps.stream().map(SolutionStep::getLevel).toList());
    }

    @Test
    void explainsTheChainRule() throws InvalidExpressionException {
        List<SolutionStep> steps = new ArrayList<>();
        calculator.derive(parser.parse("sen(x^2)"), steps, 0);

        assertEquals("Regla de la cadena con la derivada del seno: d/dx[sin(x^2)] = d/dx[x^2]*cos(x^2)",
                steps.getFirst().getDescription());
        assertEquals("Regla de la potencia: d/dx[x^2] = 2*x", steps.get(1).getDescription());
        assertEquals(1, steps.get(1).getLevel());
    }

    @Test
    void chainedSumsAndDifferencesAreOneStep() throws InvalidExpressionException {
        List<SolutionStep> steps = new ArrayList<>();
        calculator.derive(parser.parse("x^3 - 4x + ln(x) - 7"), steps, 0);

        assertEquals("Regla de la suma y la diferencia: d/dx[x^3 - 4*x + ln(x) - 7] = "
                + "d/dx[x^3] - d/dx[4*x] + d/dx[ln(x)] - d/dx[7]", steps.getFirst().getDescription());
        assertTrue(steps.stream().skip(1).allMatch(step -> step.getLevel() >= 1));
    }

    @Test
    void namesTheRulesUsedInClass() throws InvalidExpressionException {
        assertTrue(rulesFor("x*sen(x)").contains("Regla del producto"));
        assertTrue(rulesFor("sen(x)/x").contains("Regla del cociente"));
        assertTrue(rulesFor("2^x").contains("Regla de la exponencial"));
        assertTrue(rulesFor("x^x").contains("Derivación logarítmica"));
        assertTrue(rulesFor("pi^2").contains("Regla de la constante: d/dx[π^2] = 0 (no depende de x)"));
    }

    @Test
    void usesTheVariableNameInTheNotation() throws InvalidExpressionException {
        List<SolutionStep> steps = new ArrayList<>();
        new DerivativeCalculator("t").derive(new ExpressionParser("t").parse("t^2"), steps, 0);
        assertEquals("Regla de la potencia: d/dt[t^2] = 2*t", steps.getFirst().getDescription());
    }

    private String rulesFor(String text) throws InvalidExpressionException {
        List<SolutionStep> steps = new ArrayList<>();
        calculator.derive(parser.parse(text), steps, 0);
        return String.join("\n", steps.stream().map(SolutionStep::getDescription).toList());
    }
}
