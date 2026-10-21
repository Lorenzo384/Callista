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
    void onlyEvaluationIsSupportedForNow() {
        assertEquals(Set.of(OperationType.EVALUATE), model.getSupportedOperations());
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
}
