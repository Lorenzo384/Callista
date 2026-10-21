package com.callista.model;

import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.OperationType;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorModelTest {

    private static final double DELTA = 1e-9;

    private final CalculatorModel model = new CalculatorModel();

    @Test
    void supportsEvaluationAndDerivatives() {
        assertEquals(Set.of(OperationType.EVALUATE, OperationType.DERIVATIVE), model.getSupportedOperations());
    }

    @Test
    void interpretsFunctions() throws InvalidExpressionException {
        assertEquals("3*x^2 + sin(x)", model.interpret("3x^2 + sen(x)").toString());
    }

    @Test
    void interpretsValuesWrittenAsExpressions() throws InvalidExpressionException {
        assertEquals(2, model.interpretValue("2"), DELTA);
        assertEquals(-1.5, model.interpretValue("-1.5"), DELTA);
        assertEquals(Math.PI / 2, model.interpretValue("pi/2"), DELTA);
        assertEquals(Math.E * Math.E, model.interpretValue("e^2"), DELTA);
    }

    @Test
    void rejectsValuesThatAreNotRealNumbers() {
        InvalidExpressionException withVariable =
                assertThrows(InvalidExpressionException.class, () -> model.interpretValue("x + 1"));
        assertTrue(withVariable.getMessage().contains("número real"));
        assertThrows(InvalidExpressionException.class, () -> model.interpretValue("1/0"));
        assertThrows(InvalidExpressionException.class, () -> model.interpretValue("raiz(-1)"));
        assertThrows(InvalidExpressionException.class, () -> model.interpretValue("2 +"));
    }

    @Test
    void evaluatesAndExplainsTheSteps() throws InvalidExpressionException {
        Expression function = model.interpret("3x^2 + 1");
        CalculationResult result = model.evaluate(function, 2);

        assertEquals("f(2) = 13", result.getSummary());
        List<String> steps = result.getSteps().stream().map(SolutionStep::getDescription).toList();
        assertEquals(List.of(
                "Función: f(x) = 3*x^2 + 1",
                "Sustituir x = 2 (los ángulos se miden en radianes)",
                "Resultado: f(2) = 13"), steps);
    }

    @Test
    void reportsPointsOutsideTheDomain() throws InvalidExpressionException {
        CalculationResult logarithm = model.evaluate(model.interpret("ln(x)"), -1);
        assertEquals("f(-1) no está definida", logarithm.getSummary());
        assertTrue(logarithm.getSteps().getLast().getDescription().contains("fuera del dominio"));

        CalculationResult division = model.evaluate(model.interpret("1/x"), 0);
        assertEquals("f(0) no está definida", division.getSummary());
    }

    @Test
    void derivesAndExplainsTheSteps() throws InvalidExpressionException {
        CalculationResult result = model.derive(model.interpret("3x^2 + sen(x)"), 1);

        assertEquals("f'(x) = 6*x + cos(x)", result.getSummary());
        List<String> steps = result.getSteps().stream().map(SolutionStep::getDescription).toList();
        assertEquals("Función: f(x) = 3*x^2 + sin(x)", steps.getFirst());
        assertEquals("Resultado: f'(x) = 6*x + cos(x)", steps.getLast());
    }

    @Test
    void derivesSeveralTimes() throws InvalidExpressionException {
        Expression function = model.interpret("x^3 - 4x + ln(x)");
        assertEquals("f''(x) = 6*x - 1/x^2", model.derive(function, 2).getSummary());
        assertEquals("f'''(x) = 6 + 2/x^3", model.derive(function, 3).getSummary());

        List<SolutionStep> steps = model.derive(function, 2).getSteps();
        List<String> main = steps.stream().filter(s -> s.getLevel() == 0)
                .map(SolutionStep::getDescription).toList();
        assertEquals(List.of(
                "Función: f(x) = x^3 - 4*x + ln(x)",
                "Primera derivada: se deriva f(x) = x^3 - 4*x + ln(x)",
                "Segunda derivada: se deriva f'(x) = 3*x^2 - 4 + 1/x"), main);
    }

    @Test
    void evaluatesTheDerivativeAtAPoint() throws InvalidExpressionException {
        CalculationResult result = model.derive(model.interpret("x^2*sen(3x)"), 1, Math.PI);
        assertEquals("f'(3.141592654) = -29.6088132", result.getSummary());
        assertTrue(result.getSteps().getLast().getDescription().startsWith("Evaluar en x = 3.141592654"));

        CalculationResult undefined = model.derive(model.interpret("ln(x)"), 1, 0);
        assertEquals("f'(0) no está definida", undefined.getSummary());
    }

    @Test
    void rejectsOrdersOutOfRange() throws InvalidExpressionException {
        Expression function = model.interpret("x");
        assertThrows(IllegalArgumentException.class, () -> model.derive(function, 0));
        assertThrows(IllegalArgumentException.class, () -> model.derive(function, 4));
    }

    @Test
    void simplifiesTheDerivativeAndSaysHow() throws InvalidExpressionException {
        CalculationResult result = model.derive(model.interpret("x^x"), 1);

        assertEquals("f'(x) = x^x*(ln(x) + 1)", result.getSummary());
        List<String> steps = result.getSteps().stream().map(SolutionStep::getDescription).toList();
        assertEquals("Se obtiene: f'(x) = x^x*(ln(x) + x/x)", steps.get(steps.size() - 2));
        assertEquals("Simplificar (cancelar factores comunes): f'(x) = x^x*(ln(x) + 1)", steps.getLast());
    }

    @Test
    void doesNotAddASimplificationStepWhenNothingChanges() throws InvalidExpressionException {
        List<String> steps = model.derive(model.interpret("3x^2 + sen(x)"), 1).getSteps().stream()
                .map(SolutionStep::getDescription).toList();
        assertTrue(steps.stream().noneMatch(step -> step.startsWith("Simplificar")));
    }

    @Test
    void namesSeveralSimplificationsInSpanish() throws InvalidExpressionException {
        List<SolutionStep> steps = model.derive(model.interpret("x^3 - 4x + ln(x)"), 3).getSteps();
        assertTrue(steps.stream().map(SolutionStep::getDescription)
                .anyMatch(step -> step.equals("Simplificar (aplicar leyes de los exponentes): f'''(x) = 6 + 2/x^3")));
    }
}
