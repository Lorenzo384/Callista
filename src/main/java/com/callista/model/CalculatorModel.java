package com.callista.model;

import com.callista.model.entity.CalculationResult;
import com.callista.model.entity.OperationType;
import com.callista.model.entity.SolutionStep;
import com.callista.model.exception.InvalidExpressionException;
import com.callista.model.expression.Expression;
import com.callista.model.parser.ExpressionParser;
import com.callista.util.FormatUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Fachada del modelo: único punto de entrada que usan los controladores.
 * No conoce nada de la interfaz gráfica.
 */
public class CalculatorModel {

    private static final Set<OperationType> SUPPORTED = EnumSet.of(OperationType.EVALUATE);

    private final ExpressionParser parser = new ExpressionParser();

    /**
     * Operaciones ya implementadas; el resto se muestran como «próximamente».
     */
    public Set<OperationType> getSupportedOperations() {
        return SUPPORTED;
    }

    /**
     * Interpreta la función escrita por el usuario.
     *
     * @throws InvalidExpressionException si el texto no es una expresión válida
     */
    public Expression interpret(String text) throws InvalidExpressionException {
        return parser.parse(text);
    }

    /**
     * Interpreta un valor numérico. Acepta expresiones sin variable, como
     * {@code 2}, {@code -1.5} o {@code pi/2}.
     *
     * @throws InvalidExpressionException si el texto no es válido o no da un
     *                                    número real (por ejemplo, si contiene x)
     */
    public double interpretValue(String text) throws InvalidExpressionException {
        // Se evalúa con x = NaN: si el valor depende de x, el resultado será NaN.
        double value = parser.parse(text).evaluate(Double.NaN);
        if (!Double.isFinite(value)) {
            throw new InvalidExpressionException(
                    "El valor debe ser un número real, por ejemplo 2, -1.5 o pi/2", 1);
        }
        return value;
    }

    /**
     * Evalúa la función en un punto y explica el procedimiento.
     */
    public CalculationResult evaluate(Expression function, double x) {
        double y = function.evaluate(x);
        String point = FormatUtils.formatNumber(x);
        String image = "f(" + point + ")";
        boolean defined = Double.isFinite(y);

        String conclusion = defined
                ? "Resultado: " + image + " = " + FormatUtils.formatNumber(y)
                : image + " no está definida: x = " + point + " está fuera del dominio de la función";
        List<SolutionStep> steps = List.of(
                new SolutionStep("Función: f(x) = " + function),
                new SolutionStep("Sustituir x = " + point + " (los ángulos se miden en radianes)"),
                new SolutionStep(conclusion));

        String summary = defined ? image + " = " + FormatUtils.formatNumber(y) : image + " no está definida";
        return new CalculationResult(summary, steps);
    }
}
