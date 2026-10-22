package com.callista.model.calculus.area;

import com.callista.model.calculus.area.CurveIntersections.Intersection;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurveIntersectionsTest {

    private final ExpressionParser parser = new ExpressionParser();

    private List<Intersection> roots(String h, double from, double to) throws InvalidExpressionException {
        return CurveIntersections.find(parser.parse(h), from, to);
    }

    private static List<String> values(List<Intersection> roots) {
        return roots.stream().map(r -> r.value().toString()).toList();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x - x^2         | 0, 1",
            "x^3 - x         | -1, 0, 1",
            "x^2 - x - 2     | -1, 2",
            "2x^2 - 1        | -sqrt(2)/2, sqrt(2)/2",
            "4x^2 - 1        | -1/2, 1/2",
            "x^2 - x - 1     | (1 - sqrt(5))/2, (1 + sqrt(5))/2",
            "sen(x) - cos(x) | -3*π/4, π/4",
    })
    void findsCrossingsWithExactValues(String h, String expected) throws InvalidExpressionException {
        List<Intersection> roots = roots(h, -3, 3);
        assertEquals(List.of(expected.split(", ")), values(roots));
        assertTrue(roots.stream().allMatch(Intersection::exact));
    }

    @Test
    void findsPointsWhereTheCurvesTouchWithoutCrossing() throws InvalidExpressionException {
        // x^3 - x^2 = x^2 (x - 1): en x = 0 no cambia de signo
        assertEquals(List.of("0", "1"), values(roots("x^3 - x^2", -2.3, 2.7)));
        assertEquals(List.of("1"), values(roots("(x - 1)^2", -2.3, 2.7)));
    }

    @Test
    void ignoresAsymptotes() throws InvalidExpressionException {
        // 1/x cambia de signo en 0, pero ahí no vale 0
        assertTrue(roots("1/x", -1, 1).isEmpty());
        assertEquals(List.of("1"), values(roots("1/x - 1/x^2", -2, 2)));
    }

    @Test
    void roundsRootsThatAreNotSimpleValues() throws InvalidExpressionException {
        Expression h = parser.parse("e^x - 3 + x");
        List<Intersection> roots = CurveIntersections.find(h, -5, 5);
        assertEquals(1, roots.size());
        Intersection root = roots.getFirst();
        assertFalse(root.exact());
        assertEquals(0, h.evaluate(root.x()), 1e-12);
        assertEquals("0.7920599684", root.value().toString());
    }

    @Test
    void includesRootsAtTheEndsOfTheInterval() throws InvalidExpressionException {
        assertEquals(List.of("0", "1"), values(roots("x - x^2", 0, 1)));
    }

    @Test
    void recognizesSimpleExactValues() {
        assertEquals("3/4", CurveIntersections.exactValue(0.75).orElseThrow().toString());
        assertEquals("2*π/3", CurveIntersections.exactValue(2 * Math.PI / 3).orElseThrow().toString());
        assertEquals("sqrt(3)", CurveIntersections.exactValue(Math.sqrt(3)).orElseThrow().toString());
        assertTrue(CurveIntersections.exactValue(0.7920599684).isEmpty());
    }
}
