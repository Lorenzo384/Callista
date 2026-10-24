package com.callista.model.calculus.integral;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.callista.model.calculus.numeric.NumericalMethod;
import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.Graph;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.exception.ParameterException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefiniteIntegralTest {

    private final ExpressionParser parser = new ExpressionParser();
    private final DefiniteIntegral integral = new DefiniteIntegral();

    private Expression f(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    private static List<String> steps(CalculationResult result) {
        return result.getSteps().stream().map(SolutionStep::getDescription).toList();
    }

    @Test
    void explainsTheTrapezoidalRuleStepByStep() throws InvalidExpressionException, ParameterException {
        CalculationResult result = integral.approximate(f("x^2"), 0, 2, NumericalMethod.TRAPEZOIDAL, 4);

        assertEquals("∫[0, 2] f(x) dx ≈ 2.75", result.getSummary());
        List<String> steps = steps(result);
        assertTrue(steps.contains("Ancho de cada subintervalo: Δx = (b - a)/n = (2 - 0)/4 = 0.5"));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("x₁ = 0.5") && s.contains("f(x₁) = 0.25")
                && s.endsWith("peso 2")));
        assertTrue(steps.contains("Suma ponderada: Σ pesoᵢ·f(xᵢ) = 11"));
        assertTrue(steps.contains("Resultado: Δx/2·Σ = (0.5/2)·11 = 2.75"));
        assertTrue(steps.getLast().startsWith("Valor de referencia (Simpson con n = 10000): 2.666666667"));
        assertTrue(steps.getLast().endsWith("error estimado ≈ 0.08333"));
    }

    @Test
    void summarizesLongTables() throws InvalidExpressionException, ParameterException {
        List<SolutionStep> steps = integral.approximate(f("x"), 0, 1, NumericalMethod.SIMPSON, 20).getSteps();
        List<String> table = steps.stream().filter(s -> s.getLevel() == 1).map(SolutionStep::getDescription).toList();
        assertEquals(9, table.size()); // 5 primeros, «⋮» y 3 últimos
        assertEquals("⋮", table.get(5));
        assertTrue(table.getLast().startsWith("x₂₀ = 1"));
    }

    @Test
    void comparesTheThreeMethods() throws InvalidExpressionException, ParameterException {
        CalculationResult result = integral.compare(f("e^(-x^2)"), 0, 1, 5);

        assertTrue(result.getSummary().endsWith("(Simpson 1/3)"), result.getSummary());
        List<String> steps = steps(result);
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Trapecio: ≈ 0.7443683398")));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Punto medio: ≈ 0.7480532525")));
        assertTrue(steps.stream().anyMatch(s -> s.startsWith("Simpson 1/3 con n = 6 (necesita n par)")));
        assertTrue(steps.getLast().startsWith("Conclusión: el más preciso aquí es Simpson 1/3."));
    }

    @Test
    void warnsAboutImproperIntegrals() throws InvalidExpressionException, ParameterException {
        // El punto medio esquiva x = 0, pero la integral de 1/x en [-1, 1] no existe
        CalculationResult result = integral.approximate(f("1/x"), -1, 1, NumericalMethod.MIDPOINT, 4);
        assertTrue(steps(result).getLast().startsWith("Advertencia: f(x) no está definida en todo [-1, 1]"));
        assertTrue(steps(result).contains("Ancho de cada subintervalo: Δx = (b - a)/n = (1 - (-1))/4 = 0.5"));
    }

    @Test
    void cannotApproximateWhenAPointIsUndefined() throws InvalidExpressionException, ParameterException {
        CalculationResult result = integral.approximate(f("ln(x)"), -1, 1, NumericalMethod.TRAPEZOIDAL, 4);
        assertEquals("∫[-1, 1] f(x) dx: no se puede aproximar", result.getSummary());
        assertTrue(steps(result).getLast().startsWith("f(x) no está definida en x = -1"));
        assertTrue(result.getGraph().orElseThrow().areas().isEmpty());
    }

    @Test
    void graphsTheAreaAndTheShapes() throws InvalidExpressionException, ParameterException {
        Graph graph = integral.approximate(f("x^2"), 0, 2, NumericalMethod.MIDPOINT, 4).getGraph().orElseThrow();
        assertEquals(1, graph.areas().size());
        assertEquals("Área: ∫[0, 2] f(x) dx", graph.areas().getFirst().label());
        assertEquals(4, graph.polygons().size());
        assertTrue(graph.xRange().contains(0) && graph.xRange().contains(2));

        Graph fine = integral.approximate(f("x^2"), 0, 2, NumericalMethod.MIDPOINT, 100).getGraph().orElseThrow();
        assertTrue(fine.polygons().isEmpty(), "con muchos subintervalos no se dibujan las figuras");

        Graph compared = integral.compare(f("x^2"), 0, 2, 4).getGraph().orElseThrow();
        assertTrue(compared.polygons().isEmpty());
        assertEquals(1, compared.areas().size());
    }

    // ------------------------------------------------------------ exacta

    private CalculationResult exact(String function, String a, String b) throws InvalidExpressionException {
        return integral.exact(f(function), f(a), f(b));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
            "x^2          | 0 | 1        | ∫[0, 1] f(x) dx = 1/3 ≈ 0.3333333333",
            "x^2          | 0 | pi       | ∫[0, π] f(x) dx = π^3/3 ≈ 10.33542556",
            "sen(x)       | 0 | pi       | ∫[0, π] f(x) dx = 2",
            "1/x          | 1 | 2        | ∫[1, 2] f(x) dx = ln(2) ≈ 0.6931471806",
            "x*cos(x^2)   | 0 | sqrt(pi) | ∫[0, sqrt(π)] f(x) dx = 0",
            "3x^2 + 1     | 2 | 0        | ∫[2, 0] f(x) dx = -10",
            "x*e^x        | 0 | 1        | ∫[0, 1] f(x) dx = 1",
            "ln(x)        | 1 | e        | ∫[1, e] f(x) dx = 1",
            "x*sen(x)     | 0 | pi       | ∫[0, π] f(x) dx = π ≈ 3.141592654",
    })
    void computesExactValuesWithTheFundamentalTheorem(String function, String a, String b, String expected)
            throws InvalidExpressionException {
        assertEquals(expected, exact(function, a, b).getSummary());
    }

    @Test
    void explainsTheFundamentalTheorem() throws InvalidExpressionException {
        List<String> steps = steps(exact("x^2", "0", "pi"));
        assertTrue(steps.contains("Antiderivada: F(x) = x^3/3"));
        assertTrue(steps.contains("Teorema Fundamental del Cálculo: ∫[a, b] f(x) dx = F(b) - F(a)"));
        assertTrue(steps.contains("F(π) = π^3/3 ≈ 10.33542556"));
        assertTrue(steps.getLast().startsWith("Comprobación numérica"));
        assertTrue(steps.getLast().endsWith("✓"));
    }

    @Test
    void treatsAnAsymptoteInsideAsAnImproperIntegral() throws InvalidExpressionException {
        // Aplicar F(b) - F(a) a 1/x² en [-1, 1] daría -2, que es falso: la integral diverge
        CalculationResult result = exact("1/x^2", "-1", "1");
        assertEquals("∫[-1, 1] f(x) dx diverge (∞)", result.getSummary());
        assertTrue(steps(result).getLast().contains("diverge"));
    }

    @Test
    void approximatesWhenThereIsNoAntiderivative() throws InvalidExpressionException {
        CalculationResult result = exact("e^(-x^2)", "0", "1");
        assertEquals("∫[0, 1] f(x) dx ≈ 0.7468241328", result.getSummary());
        assertTrue(steps(result).stream().anyMatch(s -> s.startsWith("Como no se encontró antiderivada")));
    }


    @Test
    void acceptsLargeValuesWhenTheyAreFinite() throws InvalidExpressionException {
        CalculationResult result = exact("10000000000000", "0", "1");

        assertFalse(
                result.getSummary().contains("fuera del dominio"),
                "Una función constante con valores grandes no debe "
                        + "rechazarse por superar un umbral arbitrario."
        );
    }

}
