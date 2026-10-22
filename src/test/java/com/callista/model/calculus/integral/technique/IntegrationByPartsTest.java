package com.callista.model.calculus.integral.technique;

import com.callista.model.calculus.integral.IndefiniteIntegral;
import com.callista.model.calculus.integral.technique.IntegrationByParts.Kind;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.expression.Variable;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationByPartsTest {

    private final ExpressionParser parser = new ExpressionParser();
    private final IndefiniteIntegral integral = new IndefiniteIntegral();

    private Expression f(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    private List<String> steps(String text) throws InvalidExpressionException, UnsupportedIntegralException {
        List<SolutionStep> steps = new ArrayList<>();
        integral.antiderivative(f(text), steps, 0);
        return steps.stream().map(SolutionStep::getDescription).toList();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x*e^x            | x*e^x - e^x",
            "x*cos(x)         | x*sin(x) + cos(x)",
            "x*e^(2x)         | x*e^(2*x)/2 - e^(2*x)/4",
            "x*ln(x)          | x^2*ln(x)/2 - x^2/4",
            "x^2*ln(x)        | x^3*ln(x)/3 - x^3/9",
            "ln(x)            | x*ln(x) - x",
            "arctan(x)        | x*arctan(x) - ln(abs(x^2 + 1))/2",
            "arcsen(x)        | x*arcsin(x) + sqrt(1 - x^2)",
            // varias veces
            "x^2*e^x          | x^2*e^x - 2*x*e^x + 2*e^x",
            "x^3*e^x          | x^3*e^x - 3*x^2*e^x + 6*x*e^x - 6*e^x",
            "ln(x)^2          | x*ln(x)^2 - 2*x*ln(x) + 2*x",
            // cíclicas
            "e^x*sen(x)       | sin(x)*e^x/2 - cos(x)*e^x/2",
            "e^x*cos(x)       | cos(x)*e^x/2 + sin(x)*e^x/2",
            "e^(2x)*cos(3x)   | 2*cos(3*x)*e^(2*x)/13 + 3*sin(3*x)*e^(2*x)/13",
    })
    void integratesByParts(String function, String expected)
            throws InvalidExpressionException, UnsupportedIntegralException {
        assertEquals(expected, integral.antiderivative(f(function)).toString());
    }

    /** La derivada numérica de la antiderivada coincide con la función. */
    @ParameterizedTest
    @ValueSource(strings = {
            "x*sen(x)", "x^2*cos(x)", "x*2^x", "x*sec(x)^2", "(x + 1)*e^x", "x*senh(x)", "log(x)", "x^3*ln(x)",
            "sqrt(x)*ln(x)", "arccos(x)", "e^(-x)*sen(2x)", "e^x*cos(x)", "x^2*e^(-x)", "(2x - 1)*ln(x)",
    })
    void derivativeOfTheResultIsTheFunction(String text) throws InvalidExpressionException, UnsupportedIntegralException {
        Expression function = f(text);
        Expression antiderivative = integral.antiderivative(function);
        double h = 1e-6;
        for (double x : new double[]{0.3, 0.6, 0.9}) {
            double numeric = (antiderivative.evaluate(x + h) - antiderivative.evaluate(x - h)) / (2 * h);
            assertEquals(function.evaluate(x), numeric, 1e-4 * Math.max(1, Math.abs(numeric)),
                    () -> text + " en x = " + x + ": F = " + antiderivative);
        }
    }

    @Test
    void explainsTheChoiceOfUAndDv() throws InvalidExpressionException, UnsupportedIntegralException {
        List<String> steps = steps("x*ln(x)");
        assertEquals("Integración por partes: ∫x*ln(x) dx: u = ln(x) (logarítmica, por ILATE),  dv = x dx",
                steps.getFirst());
        assertTrue(steps.contains("Derivar u e integrar dv: du = 1/x dx,  v = x^2/2"));
        assertTrue(steps.contains("Aplicar ∫u dv = u·v - ∫v du: ∫x*ln(x) dx = x^2*ln(x)/2 - ∫x/2 dx"));
    }

    @Test
    void usesDxAsDvForASingleLogarithm() throws InvalidExpressionException, UnsupportedIntegralException {
        assertEquals("Integración por partes: ∫ln(x) dx: u = ln(x) (logarítmica, por ILATE),  dv = dx",
                steps("ln(x)").getFirst());
    }

    @Test
    void solvesCyclicIntegrals() throws InvalidExpressionException, UnsupportedIntegralException {
        List<String> steps = steps("e^x*sen(x)");
        assertTrue(steps.getLast().startsWith("La integral original vuelve a aparecer: con I = ∫e^x*sin(x) dx: I = "),
                steps.getLast());
        assertTrue(steps.getLast().contains(" - I  ⇒  I = "), steps.getLast());
    }

    @Test
    void discardsTheSecondStepWhenTheIntegralDoesNotReappear() throws InvalidExpressionException,
            UnsupportedIntegralException {
        // En x^2*e^x el segundo intento cíclico no funciona y sus pasos no deben quedar repetidos
        long choices = steps("x^2*e^x").stream().filter(s -> s.startsWith("Integración por partes")).count();
        assertEquals(2, choices);
    }

    @Test
    void classifiesFactorsWithIlate() {
        Variable x = new Variable("x");
        Constant one = new Constant(1);
        assertEquals(Kind.INVERSE_TRIGONOMETRIC, IntegrationByParts.kind(new FunctionCall(MathFunction.ARCTAN, x), one));
        assertEquals(Kind.LOGARITHMIC, IntegrationByParts.kind(new FunctionCall(MathFunction.LN, x), one));
        assertEquals(Kind.ALGEBRAIC, IntegrationByParts.kind(x, new Constant(3)));
        assertEquals(Kind.TRIGONOMETRIC, IntegrationByParts.kind(new FunctionCall(MathFunction.COS, x), one));
        assertEquals(Kind.EXPONENTIAL, IntegrationByParts.kind(Constant.E, x));
        assertTrue(Kind.INVERSE_TRIGONOMETRIC.compareTo(Kind.EXPONENTIAL) < 0, "ILATE: I antes que E");
    }
}
