package com.callista.model.calculus.area;

import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.CurveStyle;
import com.callista.model.entity.Graph;
import com.callista.model.entity.GraphArea;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaBetweenCurvesTest {

    private final ExpressionParser parser = new ExpressionParser();
    private final AreaBetweenCurves area = new AreaBetweenCurves();

    private Expression e(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    private static List<String> steps(CalculationResult result) {
        return result.getSteps().stream().map(SolutionStep::getDescription).toList();
    }

    /** ∫[a, b] |f - g| dx con Simpson, para comparar con el resultado. */
    private double numericArea(String f, String g, double a, double b) throws Exception {
        Expression absolute = new FunctionCall(MathFunction.ABS, new Difference(e(f), e(g)));
        return NumericalMethod.SIMPSON.getRule().integrate(absolute, a, b, 20_000).value();
    }

    private static double approximation(CalculationResult result) {
        String summary = result.getSummary();
        return Double.parseDouble(summary.substring(summary.indexOf('≈') + 1).trim());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x          | x^2       | Área = 1/6 ≈ 0.1666666667     | 0  | 1",
            "raiz(x)    | x^2       | Área = 1/3 ≈ 0.3333333333     | 0  | 1",
            "x^2        | x + 2     | Área = 9/2 ≈ 4.5              | -1 | 2",
            "x^2        | 2 - x^2   | Área = 8/3 ≈ 2.666666667      | -1 | 1",
            "x^2 - 4    | 0         | Área = 32/3 ≈ 10.66666667     | -2 | 2",
            "x^3        | x         | Área = 1/2 ≈ 0.5              | -1 | 1",
            "x^3 - x^2  | 0         | Área = 1/12 ≈ 0.08333333333   | 0  | 1",
            "x^2        | x + 1     | Área = 5*sqrt(5)/6 ≈ 1.863389981 | -0.618034 | 1.618034",
            "x^4        | 2x^2      | Área = 16*sqrt(2)/15 ≈ 1.508494467 | -1.4142136 | 1.4142136",
    })
    void computesTheEnclosedRegion(String f, String g, String expected, double a, double b) throws Exception {
        CalculationResult result = area.enclosed(e(f), e(g));
        assertEquals(expected, result.getSummary());
        assertEquals(numericArea(f, g, a, b), approximation(result), 1e-5);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "sen(x)  | cos(x) | 0    | pi   | Área = 2*sqrt(2) ≈ 2.828427125",
            "x       | 0      | -1   | 2    | Área = 5/2 ≈ 2.5",
            "sen(x)  | 0      | 0    | 2pi  | Área = 4",
            "e^x     | x + 1  | 0    | 1    | Área = e - 5/2 ≈ 0.2182818285",
            "x^2     | x      | 0    | 2    | Área = 1",
            "1/x     | 0      | 1    | e    | Área = 1",
    })
    void computesTheAreaInAnInterval(String f, String g, String a, String b, String expected) throws Exception {
        CalculationResult result = area.between(e(f), e(g), e(a), e(b));
        assertEquals(expected, result.getSummary());
        double value = expected.contains("≈") ? approximation(result)
                : Double.parseDouble(expected.substring(expected.indexOf('=') + 1).trim());
        assertEquals(numericArea(f, g, e(a).evaluate(0), e(b).evaluate(0)), value, 1e-5);
    }

    @Test
    void explainsEachPieceAndWhichCurveIsOnTop() throws Exception {
        List<String> steps = steps(area.enclosed(e("x^3"), e("x")));
        assertTrue(steps.contains("Puntos de corte: f(x) = g(x) ⇔ f(x) - g(x) = x^3 - x = 0"));
        assertTrue(steps.contains("x = -1  →  (-1, -1)"));
        assertTrue(steps.contains("La región encerrada va del primer al último corte: [-1, 1]"));
        assertTrue(steps.contains("H(x) = x^4/4 - x^2/2"));
        assertTrue(steps.contains("Tramo 1: [-1, 0]: en x = -0.5, f = -0.125 y g = -0.5, así que f va arriba"));
        assertTrue(steps.contains("Tramo 2: [0, 1]: en x = 0.5, f = 0.125 y g = 0.5, así que g va arriba"));
        assertTrue(steps.contains("A₂ = ∫[0, 1] (g(x) - f(x)) dx = ∫[0, 1] (-x^3 + x) dx"));
        assertTrue(steps.contains("= H(0) - H(1) = 1/4 ≈ 0.25"));
        assertTrue(steps.contains("Área total: A = A₁ + A₂ = 1/2 ≈ 0.5"));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Sin valor absoluto, ∫[-1, 1] (f(x) - g(x)) dx ≈ 0")));
        assertTrue(steps.getLast().endsWith("≈ 0.5 ✓"), steps.getLast());
    }

    @Test
    void splitsTheIntervalAtTheCrossingsInside() throws Exception {
        List<String> steps = steps(area.between(e("sen(x)"), e("cos(x)"), e("0"), e("pi")));
        assertTrue(steps.contains("x = π/4 ≈ 0.7854  →  (0.7854, 0.7071)"));
        assertTrue(steps.contains("= H(0) - H(π/4) = sqrt(2) - 1 ≈ 0.4142135624"));
        assertTrue(steps.contains("= H(π) - H(π/4) = 1 + sqrt(2) ≈ 2.414213562"));
    }

    @Test
    void usesASinglePieceWhenTheCurvesDoNotCrossInside() throws Exception {
        CalculationResult result = area.between(e("x^2 + 1"), e("x"), e("0"), e("2"));
        assertEquals("Área = 8/3 ≈ 2.666666667", result.getSummary());
        List<String> steps = steps(result);
        assertTrue(steps.getFirst().equals("Curvas: f(x) = x^2 + 1 y g(x) = x"));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Puntos de corte dentro de (0, 2): ninguno")));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Tramo [0, 2]:")));
        assertTrue(steps.stream().noneMatch(s -> s.startsWith("Sin valor absoluto")));
    }

    @Test
    void approximatesWhenThereIsNoElementaryAntiderivative() throws Exception {
        CalculationResult result = area.between(e("e^(-x^2)"), e("0"), e("0"), e("1"));
        assertEquals("Área ≈ 0.7468241328", result.getSummary());
        assertTrue(steps(result).stream().anyMatch(s -> s.contains("(Simpson, n = 1000)")));
    }

    @Test
    void approximatesWhenTheCrossingsAreNotSimpleValues() throws Exception {
        // e^x = 3 - x en x ≈ 0.792
        CalculationResult result = area.between(e("e^x"), e("3 - x"), e("0"), e("2"));
        assertTrue(result.getSummary().startsWith("Área ≈ "), result.getSummary());
        assertEquals(numericArea("e^x", "3 - x", 0, 2), approximation(result), 1e-6);
        assertTrue(steps(result).stream().anyMatch(s -> s.contains("(aproximado)")));
    }

    @Test
    void explainsWhenTheIntegralIsImproper() throws Exception {
        CalculationResult result = area.between(e("1/x"), e("0"), e("-1"), e("1"));
        assertEquals("Área entre curvas: integral impropia", result.getSummary());
    }

    @Test
    void drawsBothCurvesTheCrossingsAndOneRegionPerPiece() throws Exception {
        Graph graph = area.enclosed(e("x^3"), e("x")).getGraph().orElseThrow();
        assertEquals(List.of(CurveStyle.FUNCTION, CurveStyle.SECOND_FUNCTION),
                graph.curves().stream().map(c -> c.style()).toList());
        assertEquals(3, graph.points().size());
        assertEquals(2, graph.areas().size());
        GraphArea left = graph.areas().getFirst();
        GraphArea right = graph.areas().getLast();
        assertEquals(e("x^3"), left.upper()); // en [-1, 0] f va arriba
        assertEquals(e("x"), right.upper()); // en [0, 1] g va arriba
        assertEquals(-1, left.from(), 1e-12);
        assertEquals(1, right.to(), 1e-12);
        assertTrue(graph.areas().stream().noneMatch(GraphArea::touchesXAxis));
    }

    @Test
    void rejectsCurvesThatDoNotEncloseARegion() throws InvalidExpressionException {
        ParameterException once = assertThrows(ParameterException.class, () -> area.enclosed(e("e^x"), e("x + 1")));
        assertTrue(once.getMessage().contains("solo se cortan en x = 0"), once.getMessage());
        ParameterException never = assertThrows(ParameterException.class, () -> area.enclosed(e("x^2 + 1"), e("0")));
        assertTrue(never.getMessage().contains("no se cortan"), never.getMessage());
        ParameterException many = assertThrows(ParameterException.class, () -> area.enclosed(e("sen(x)"), e("cos(x)")));
        assertTrue(many.getMessage().contains("escribe a y b"), many.getMessage());
    }

    @Test
    void rejectsInvalidParameters() {
        assertThrows(ParameterException.class, () -> area.enclosed(e("x^2"), e("x*x")));
        assertThrows(ParameterException.class, () -> area.between(e("x"), e("0"), e("2"), e("1")));
        assertThrows(ParameterException.class, () -> area.between(e("x"), e("0"), e("1"), e("1")));
    }
}
