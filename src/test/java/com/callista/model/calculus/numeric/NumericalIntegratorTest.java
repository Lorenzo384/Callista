package com.callista.model.calculus.numeric;

import com.callista.model.calculus.numeric.NumericalIntegration.Node;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericalIntegratorTest {

    private static final double DELTA = 1e-9;

    private final ExpressionParser parser = new ExpressionParser();

    private Expression f(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    private double integrate(NumericalMethod method, String function, double a, double b, int n)
            throws InvalidExpressionException, ParameterException {
        return method.getRule().integrate(f(function), a, b, n).value();
    }

    /** ∫[0, 2] x² dx = 8/3, calculado a mano con n = 4. */
    @ParameterizedTest
    @CsvSource({
            "TRAPEZOIDAL, 2.75",
            "MIDPOINT,    2.625",
            "SIMPSON,     2.6666666666666665",
    })
    void matchesHandCalculations(NumericalMethod method, double expected)
            throws InvalidExpressionException, ParameterException {
        assertEquals(expected, integrate(method, "x^2", 0, 2, 4), DELTA);
    }

    @Test
    void eachMethodIsExactForTheRightPolynomials() throws InvalidExpressionException, ParameterException {
        // Trapecio y punto medio son exactos para rectas; Simpson, hasta para cúbicas
        assertEquals(6, integrate(NumericalMethod.TRAPEZOIDAL, "2x + 1", 0, 2, 3), DELTA);
        assertEquals(6, integrate(NumericalMethod.MIDPOINT, "2x + 1", 0, 2, 3), DELTA);
        assertEquals(4, integrate(NumericalMethod.SIMPSON, "x^3", 0, 2, 2), DELTA);
    }

    /**
     * Al duplicar n, el error del trapecio se divide por ~4 (orden 2) y el
     * de Simpson por ~16 (orden 4).
     */
    @Test
    void errorsShrinkAtTheExpectedRate() throws InvalidExpressionException, ParameterException {
        double exact = 1 - Math.cos(1); // ∫[0, 1] sen(x) dx
        double trapezoid10 = Math.abs(integrate(NumericalMethod.TRAPEZOIDAL, "sen(x)", 0, 1, 10) - exact);
        double trapezoid20 = Math.abs(integrate(NumericalMethod.TRAPEZOIDAL, "sen(x)", 0, 1, 20) - exact);
        double simpson10 = Math.abs(integrate(NumericalMethod.SIMPSON, "sen(x)", 0, 1, 10) - exact);
        double simpson20 = Math.abs(integrate(NumericalMethod.SIMPSON, "sen(x)", 0, 1, 20) - exact);
        assertEquals(4, trapezoid10 / trapezoid20, 0.1);
        assertEquals(16, simpson10 / simpson20, 0.5);
    }

    @ParameterizedTest
    @EnumSource(NumericalMethod.class)
    void approximatesKnownIntegrals(NumericalMethod method) throws InvalidExpressionException, ParameterException {
        assertEquals(2, integrate(method, "sen(x)", 0, Math.PI, 200), 1e-4);
        assertEquals(Math.E - 1, integrate(method, "e^x", 0, 1, 200), 1e-4);
        assertEquals(Math.log(2), integrate(method, "1/x", 1, 2, 200), 1e-4);
    }

    @ParameterizedTest
    @EnumSource(NumericalMethod.class)
    void swappingTheLimitsChangesTheSign(NumericalMethod method) throws InvalidExpressionException, ParameterException {
        assertEquals(-integrate(method, "x^2 + 1", 0, 3, 6), integrate(method, "x^2 + 1", 3, 0, 6), DELTA);
    }

    @Test
    void usesTheRightPointsAndWeights() throws InvalidExpressionException, ParameterException {
        List<Integer> simpson = NumericalMethod.SIMPSON.getRule().integrate(f("x"), 0, 1, 6).nodes()
                .stream().map(Node::weight).toList();
        assertEquals(List.of(1, 4, 2, 4, 2, 4, 1), simpson);

        List<Integer> trapezoid = NumericalMethod.TRAPEZOIDAL.getRule().integrate(f("x"), 0, 1, 4).nodes()
                .stream().map(Node::weight).toList();
        assertEquals(List.of(1, 2, 2, 2, 1), trapezoid);

        List<Double> midpoints = NumericalMethod.MIDPOINT.getRule().integrate(f("x"), 0, 1, 4).nodes()
                .stream().map(Node::x).toList();
        assertEquals(List.of(0.125, 0.375, 0.625, 0.875), midpoints);
    }

    @Test
    void simpsonNeedsAnEvenNumberOfSubintervals() {
        ParameterException error = assertThrows(ParameterException.class,
                () -> integrate(NumericalMethod.SIMPSON, "x", 0, 1, 5));
        assertTrue(error.getMessage().contains("par"));
        assertTrue(error.getMessage().contains("n = 6"));
    }

    @ParameterizedTest
    @EnumSource(NumericalMethod.class)
    void rejectsSubintervalsOutOfRange(NumericalMethod method) {
        assertThrows(ParameterException.class, () -> integrate(method, "x", 0, 1, 0));
        assertThrows(ParameterException.class, () -> integrate(method, "x", 0, 1, WeightedRule.MAX_SUBINTERVALS + 2));
    }

    @Test
    void detectsPointsWhereTheFunctionIsUndefined() throws InvalidExpressionException, ParameterException {
        NumericalIntegration result = NumericalMethod.TRAPEZOIDAL.getRule().integrate(f("ln(x)"), -1, 1, 4);
        assertFalse(result.isDefined());
        assertEquals(-1, result.firstUndefinedNode().orElseThrow().x(), DELTA);
    }

    @Test
    void drawsOneShapePerSubintervalOrPair() throws InvalidExpressionException {
        Expression square = f("x^2");
        assertEquals(4, NumericalMethod.TRAPEZOIDAL.getRule().shapes(square, 0, 2, 4).size());
        assertEquals(4, NumericalMethod.MIDPOINT.getRule().shapes(square, 0, 2, 4).size());
        assertEquals(2, NumericalMethod.SIMPSON.getRule().shapes(square, 0, 2, 4).size());

        // La parábola de Simpson pasa por los tres puntos de la curva
        var parabola = NumericalMethod.SIMPSON.getRule().shapes(square, 0, 2, 2).getFirst().vertices();
        assertEquals(0, parabola.get(1).y(), DELTA);   // x = 0
        assertEquals(4, parabola.get(parabola.size() - 2).y(), DELTA); // x = 2
    }
}
