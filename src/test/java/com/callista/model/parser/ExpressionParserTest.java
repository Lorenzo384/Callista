package com.callista.model.parser;

import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionParserTest {

    private static final double DELTA = 1e-9;

    private final ExpressionParser parser = new ExpressionParser();

    private Expression parse(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    /** Escribe la expresión de vuelta en el formato normalizado. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "3x^2+1              | 3*x^2 + 1",
            "  3 · x             | 3*x",
            "2sen(x)             | 2*sin(x)",
            "x(x+1)              | x*(x + 1)",
            "(x+1)(x-1)          | (x + 1)*(x - 1)",
            "2pi                 | 2*π",
            "e^x                 | e^x",
            "2^3^2               | 2^(3^2)",
            "(2^3)^2             | (2^3)^2",
            "-x^2                | -x^2",
            "(-x)^2              | (-x)^2",
            "x^-1                | x^(-1)",
            "x/2/3               | x/2/3",
            "x-(x+1)             | x - (x + 1)",
            "+x                  | x",
            "SEN(x)^2            | sin(x)^2",
            "raiz(x+1)           | sqrt(x + 1)",
            "sen(x)cos(x)        | sin(x)*cos(x)",
    })
    void parsesToNormalizedText(String input, String expected) throws InvalidExpressionException {
        assertEquals(expected, parse(input).toString());
    }

    @Test
    void evaluatesParsedExpressions() throws InvalidExpressionException {
        assertEquals(13, parse("3x^2 + 1").evaluate(2), DELTA);
        assertEquals(512, parse("2^3^2").evaluate(0), DELTA);
        assertEquals(-9, parse("-x^2").evaluate(3), DELTA);
        assertEquals(0.5, parse("x^-1").evaluate(2), DELTA);
        assertEquals(1, parse("x/2/3").evaluate(6), DELTA);
        assertEquals(2 * Math.PI, parse("2π").evaluate(0), DELTA);
    }

    @Test
    void pythagoreanIdentityIsAlwaysOne() throws InvalidExpressionException {
        Expression identity = parse("sen(x)^2 + cos(x)^2");
        for (double x = -3; x <= 3; x += 0.7) {
            assertEquals(1, identity.evaluate(x), DELTA);
        }
    }

    @Test
    void implicitMultiplicationGroupsLeftToRight() throws InvalidExpressionException {
        // Como en la mayoría de calculadoras: 1/2x = (1/2)*x y e^2x = (e^2)*x
        assertEquals("1/2*x", parse("1/2x").toString());
        assertEquals(2, parse("1/2x").evaluate(4), DELTA);
        assertEquals("e^2*x", parse("e^2x").toString());
    }

    @Test
    void supportsOtherVariableNames() throws InvalidExpressionException {
        Expression expression = new ExpressionParser("t").parse("t^2 + 1");
        assertEquals("t^2 + 1", expression.toString());
        assertEquals(10, expression.evaluate(3), DELTA);
    }

    @Test
    void rejectsReservedVariableNames() {
        assertThrows(IllegalArgumentException.class, () -> new ExpressionParser("e"));
        assertThrows(IllegalArgumentException.class, () -> new ExpressionParser("sen"));
        assertThrows(IllegalArgumentException.class, () -> new ExpressionParser("x1"));
    }

    /** Cada error indica qué falló y en qué posición. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "''         | 1 | La expresión está vacía",
            "3x^2 +     | 6 | Falta un término después de «+»",
            "sen(x      | 4 | Falta cerrar el paréntesis abierto",
            "(x+1       | 1 | Falta cerrar el paréntesis abierto",
            "x)         | 2 | Paréntesis «)» sin abrir",
            "2 3        | 3 | Falta un operador antes de «3»",
            "()         | 1 | Paréntesis vacíos",
            "*3         | 1 | La expresión no puede empezar con «*»",
            "3*/2       | 3 | Falta un término entre «*» y «/»",
            "2 $ x      | 3 | Carácter no válido «$»",
            "cosa(x)    | 1 | Símbolo desconocido «cosa»",
            "xsen(x)    | 1 | ¿quisiste decir x*sen?",
            "y+1        | 1 | Variable desconocida «y»",
            "sen x      | 1 | La función «sen» necesita paréntesis",
            "sen^2(x)   | 1 | escribe sen(x)^2",
            "0,5        | 2 | usa punto",
    })
    void reportsErrorsWithPosition(String input, int position, String message) {
        InvalidExpressionException error = assertThrows(InvalidExpressionException.class, () -> parse(input));
        assertEquals(position, error.getPosition());
        assertTrue(error.getMessage().contains(message),
                () -> "Mensaje: " + error.getMessage());
        assertTrue(error.getMessage().endsWith("(posición " + position + ")"));
    }
}
