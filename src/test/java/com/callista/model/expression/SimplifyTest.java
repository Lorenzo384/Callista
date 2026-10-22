package com.callista.model.expression;

import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.parser.ExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simplificación completa ({@link Simplifier#simplify}).
 */
class SimplifyTest {

    private final ExpressionParser parser = new ExpressionParser();

    private String simplify(String text) throws InvalidExpressionException {
        return Simplifier.simplify(parser.parse(text)).toString();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // términos semejantes
            "2x + 3x                | 5*x",
            "x + 1 - x              | 1",
            "2(x+1) + 3(x+1)        | 5*(x + 1)",
            "x/2 + x/3              | 5*x/6",
            "0.5x + 0.25x           | 3*x/4",
            // leyes de los exponentes
            "x*x                    | x^2",
            "x^2*x^3                | x^5",
            "(x^2)^2                | x^4",
            "(2x^3)^2               | 4*x^6",
            "x^2/x^5                | 1/x^3",
            "e^x*e^(2x)             | e^(3*x)",
            "x^0                    | 1",
            // cancelación
            "x/x                    | 1",
            "(x+1)/(x+1)            | 1",
            "x/(x*2)                | 1/2",
            "2*x/(x^2)^2            | 2/x^3",
            // números y valores conocidos
            "3*(x + x)              | 6*x",
            "0*x + 5                | 5",
            "4^(1/2)*x              | 2*x",
            "2^3^2                  | 512",
            "sen(0) + ln(e) + cos(pi) | 0",
            // valores notables y radicales
            "cos(pi/4)              | sqrt(2)/2",
            "sen(pi/6)              | 1/2",
            "tan(pi/6)              | sqrt(3)/3",
            "sec(pi/6)              | 2*sqrt(3)/3",
            "cos(5pi/6)             | -sqrt(3)/2",
            "2cos(pi/4) + 2sen(pi/4) | 2*sqrt(2)",
            "sen(pi/12)             | sin(π/12)",
            "tan(pi/2)              | tan(π/2)",
            "sen(1)                 | sin(1)",
            "5^(3/2)                | 5*sqrt(5)",
            "2^(5/2)                | 4*sqrt(2)",
            "raiz(5)*raiz(5)        | 5",
            "raiz(5)*raiz(5) + 1    | 6",
            "1/3*x^(-2/3)           | 1/(3*x^(2/3))",
            // orden y signos
            "x - (2x - 3x^2)        | 3*x^2 - x",
            "1 + (3*x)^2            | 9*x^2 + 1",
            "-1/x^2 + 6x            | 6*x - 1/x^2",
            "-ln(x) + 1             | 1 - ln(x)",
            "pi*x*2                 | 2*π*x",
            "x^-2                   | 1/x^2",
            "-(x+1)                 | -x - 1",
            "0.5403023059*x + 1     | 0.5403023059*x + 1",
    })
    void simplifiesToTextbookForm(String input, String expected) throws InvalidExpressionException {
        assertEquals(expected, simplify(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "x", "3*x^2 + sin(x)", "x*cos(x) + sin(x)", "e^x*cos(x) - e^x*sin(x)",
            "(x*cos(x) - sin(x))/x^2", "6*x*(x^2 + 1)^2", "2*x/(x^2 + 1)",
    })
    void leavesSimpleExpressionsAlone(String text) throws InvalidExpressionException {
        assertEquals(text, simplify(text));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "sen(x)^2 + cos(x)^2  | identidades trigonométricas fuera de alcance",
            "1/0                  | división entre cero",
    })
    void doesNotChangeWhatItShouldNot(String text, String reason) throws InvalidExpressionException {
        assertEquals(parser.parse(text).toString(), simplify(text), reason);
    }

    @Test
    void squareRootOfASquareIsNotSimplifiedToX() throws InvalidExpressionException {
        // √(x²) = |x|, no x: solo cambia la forma de escribir el exponente 1/2
        assertEquals("sqrt(x^2)", simplify("(x^2)^(1/2)"));
        assertEquals("sqrt(x^2)", simplify("sqrt(x^2)"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "sqrt(x)*sqrt(x)      | x",
            "sqrt(x)*x            | x^(3/2)",
            "x^(1/2)              | sqrt(x)",
            "1/sqrt(x)            | 1/sqrt(x)",
            "x/sqrt(x)            | sqrt(x)",
    })
    void treatsSquareRootsAsPowersOfOneHalf(String input, String expected) throws InvalidExpressionException {
        assertEquals(expected, simplify(input));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x*(x + 1)            | x^2 + x",
            "(x + 1)^2            | x^2 + 2*x + 1",
            "(x - 1)*(x + 1)      | x^2 - 1",
            "(x^2 + 1)/x          | x + 1/x",
            "2*(x + 3) - 6        | 2*x",
            "(1 + raiz(5))^2      | 6 + 2*sqrt(5)",
            "(1 - raiz(5))^3/24 - (1 + raiz(5))^3/24 | -2*sqrt(5)/3",
    })
    void expandsProductsOfSums(String input, String expected) throws InvalidExpressionException {
        assertEquals(expected, Simplifier.expand(parser.parse(input)).toString());
    }

    @Test
    void exposesTheTerms() throws InvalidExpressionException {
        var terms = Simplifier.terms(parser.parse("3*pi*x^2 - sin(x)"));
        assertEquals(2, terms.size());
        assertEquals("3*π", terms.getFirst().constantPart().toExpression().toString());
        assertEquals("x^2", terms.getFirst().variablePart().toExpression().toString());
        assertEquals("-sin(x)", terms.get(1).toExpression().toString());
    }

    /**
     * La simplificación no cambia el valor: se compara en varios puntos.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "2x + 3x - x/2", "(x^2)^3*x^-4", "x^x*(ln(x) + x/x)", "(2x^3)^2/(4x)", "e^x*e^(2x)/e^x",
            "2(x+1) - (x+1)/3", "(x^2 + 1)/(x^2 + 1)^3", "sqrt(x)*sqrt(x)", "x^(1/3)*x^(2/3)",
            "1 + (3*x)^2 - 9x^2", "sen(2x)*sen(2x)/sen(2x)", "-(x - (2 - x))", "3^x*3^x",
    })
    void keepsTheValue(String text) throws InvalidExpressionException {
        Expression original = parser.parse(text);
        Expression simplified = Simplifier.simplify(original);
        for (double x : new double[]{0.4, 1.3, 2.7}) {
            assertEquals(original.evaluate(x), simplified.evaluate(x), 1e-9 * Math.max(1, Math.abs(original.evaluate(x))),
                    () -> text + " → " + simplified);
        }
    }

    /**
     * Simplificar dos veces da lo mismo que simplificar una.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "2x + 3x", "x^x*(ln(x) + x/x)", "2*x/(x^2)^2 + 6", "x - (2x - 3x^2)", "(1/x*x - ln(x))/x^2",
            "cos(x)*(2*x*e^x + x^2*e^x) - x^2*e^x*sin(x)", "1/3*x^(-2/3)",
    })
    void isIdempotent(String text) throws InvalidExpressionException {
        Expression once = Simplifier.simplify(parser.parse(text));
        assertEquals(once, Simplifier.simplify(once));
    }

    @Test
    void reportsWhatWasDone() throws InvalidExpressionException {
        assertEquals(Set.of(SimplificationRule.LIKE_TERMS), rules("2x + 3x"));
        assertEquals(Set.of(SimplificationRule.EXPONENT_LAWS), rules("x*x^2"));
        assertEquals(Set.of(SimplificationRule.CANCELLATION), rules("x/x"));
        assertTrue(rules("x + sen(0)").contains(SimplificationRule.KNOWN_VALUES));
        assertTrue(rules("3*x^2 + 1").isEmpty());
    }

    @Test
    void cleansUpDerivatives() throws InvalidExpressionException {
        DerivativeCalculator calculator = new DerivativeCalculator();
        assertEquals("x^x*(ln(x) + 1)", Simplifier.simplify(calculator.derive(parser.parse("x^x"))).toString());
        assertEquals("-2/x^3", Simplifier.simplify(calculator.derive(parser.parse("x^-2"))).toString());
        assertEquals("1/(3*x^(2/3))", Simplifier.simplify(calculator.derive(parser.parse("x^(1/3)"))).toString());
        assertEquals("(1 - ln(x))/x^2", Simplifier.simplify(calculator.derive(parser.parse("ln(x)/x"))).toString());
        assertEquals("x/sqrt(x^2 + 1)", Simplifier.simplify(calculator.derive(parser.parse("raiz(x^2+1)"))).toString());
        assertEquals("3/(9*x^2 + 1)", Simplifier.simplify(calculator.derive(parser.parse("arctan(3x)"))).toString());
    }

    private Set<SimplificationRule> rules(String text) throws InvalidExpressionException {
        Set<SimplificationRule> applied = EnumSet.noneOf(SimplificationRule.class);
        Simplifier.simplify(parser.parse(text), applied);
        return applied;
    }
}
