package com.callista.model.graph;

import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.model.graph.FunctionSampler.Sample;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matemática de la gráfica: intervalos, marcas de los ejes y muestreo.
 */
class GraphMathTest {

    private static final double DELTA = 1e-9;

    private final ExpressionParser parser = new ExpressionParser();

    private Expression f(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    // ------------------------------------------------------------ Range

    @Test
    void rangeRejectsInvalidIntervals() {
        assertThrows(IllegalArgumentException.class, () -> new Range(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Range(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new Range(0, Double.POSITIVE_INFINITY));
    }

    @Test
    void rangeZoomsAroundAnAnchorAndShifts() {
        Range range = new Range(-10, 10);
        assertEquals(new Range(-5, 5), range.scale(0.5, 0));
        assertEquals(new Range(-6, 4), range.scale(0.5, -2)); // el punto -2 queda fijo
        assertEquals(new Range(-7, 13), range.shift(3));
        assertEquals(new Range(1, 13), Range.centeredAt(7, 12));
        assertEquals(20, range.width(), DELTA);
        assertTrue(range.contains(0));
    }

    // ------------------------------------------------------------ NiceTicks

    @ParameterizedTest
    @CsvSource({
            "-10, 10, 8, 5",
            "0, 1, 5, 0.2",
            "-3, 7, 8, 2",
            "0, 1000, 6, 200",
            "0, 0.03, 6, 0.005",
    })
    void chooseRoundSteps(double min, double max, int maxTicks, double expected) {
        assertEquals(expected, NiceTicks.step(new Range(min, max), maxTicks), DELTA);
    }

    @Test
    void listsTicksInsideTheRange() {
        assertEquals(List.of(-10.0, -5.0, 0.0, 5.0, 10.0), NiceTicks.ticks(new Range(-10, 10), 8));
        assertEquals(List.of(-2.0, 0.0, 2.0, 4.0, 6.0), NiceTicks.ticks(new Range(-3, 7), 8));
    }

    @Test
    void formatsTicksWithTheRightDecimals() {
        assertEquals("0.6", NiceTicks.format(0.6000000000000001, 0.2));
        assertEquals("-5", NiceTicks.format(-5, 5));
        assertEquals("0.005", NiceTicks.format(0.005, 0.005));
        assertEquals("1000", NiceTicks.format(1000, 200));
    }

    // ------------------------------------------------------------ FunctionSampler

    @Test
    void samplesEvenlyAndMarksUndefinedPoints() throws InvalidExpressionException {
        List<Sample> samples = FunctionSampler.sample(f("ln(x)"), new Range(-1, 1), 5);
        assertEquals(5, samples.size());
        assertEquals(List.of(-1.0, -0.5, 0.0, 0.5, 1.0), samples.stream().map(Sample::x).toList());
        assertTrue(Double.isNaN(samples.get(0).y()));
        assertTrue(Double.isNaN(samples.get(2).y())); // ln(0) = -∞ también se trata como indefinido
        assertEquals(0, samples.get(4).y(), DELTA);
    }

    @Test
    void suggestsAVerticalScaleThatShowsTheCurve() throws InvalidExpressionException {
        Range parabola = FunctionSampler.suggestYRange(List.of(f("x^2")), new Range(-10, 10));
        assertTrue(parabola.min() <= 0 && parabola.max() >= 95, parabola::toString);

        Range constant = FunctionSampler.suggestYRange(List.of(f("3")), new Range(-10, 10));
        assertTrue(constant.contains(3) && constant.width() >= 2, constant::toString);

        Range nothing = FunctionSampler.suggestYRange(List.of(f("ln(x)")), new Range(-10, -1));
        assertTrue(nothing.contains(0), nothing::toString);
    }

    @Test
    void asymptotesDoNotFlattenTheGraph() throws InvalidExpressionException {
        Range tangent = FunctionSampler.suggestYRange(List.of(f("tan(x)"), f("sec(x)^2")), new Range(-10, 10));
        assertTrue(tangent.width() < 40, tangent::toString);

        Range reciprocal = FunctionSampler.suggestYRange(List.of(f("1/x")), new Range(-10, 10));
        assertTrue(reciprocal.width() < 40, reciprocal::toString);
    }

    @Test
    void keepsExtraValuesVisible() throws InvalidExpressionException {
        Range range = FunctionSampler.suggestYRange(List.of(f("sin(x)")), new Range(-5, 5), 30);
        assertTrue(range.contains(30), range::toString);
    }

    @Test
    void splitsCurvesWhereTheyAreUndefinedOrJump() {
        List<Sample> samples = List.of(
                new Sample(0, 1), new Sample(1, 2), new Sample(2, Double.NaN),
                new Sample(3, 1), new Sample(4, 2), new Sample(5, 100), new Sample(6, 101));
        List<List<Sample>> segments = FunctionSampler.segments(samples, 10);
        assertEquals(3, segments.size());
        assertEquals(List.of(0.0, 1.0), segments.get(0).stream().map(Sample::x).toList());
        assertEquals(List.of(3.0, 4.0), segments.get(1).stream().map(Sample::x).toList());
        assertEquals(List.of(5.0, 6.0), segments.get(2).stream().map(Sample::x).toList());
    }
}
