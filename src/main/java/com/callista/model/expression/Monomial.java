package com.callista.model.expression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un término de una expresión simplificada: un coeficiente racional por un
 * producto de factores {@code base^exponente}. Por ejemplo, {@code 3*x^2*sin(x)}
 * es el coeficiente 3 con los factores {@code {x: 2, sin(x): 1}}.
 *
 * <p>Lo produce {@link Simplifier#terms(Expression)}; el integrador lo usa
 * para separar la parte constante y reconocer formas como {@code c·xⁿ}.</p>
 *
 * @param coefficient coeficiente racional
 * @param factors     bases con su exponente, en orden de aparición
 */
public record Monomial(Fraction coefficient, Map<Expression, Expression> factors) {

    public Monomial {
        factors = Collections.unmodifiableMap(new LinkedHashMap<>(factors));
    }

    /**
     * El coeficiente y los factores que no dependen de la variable:
     * en {@code 3*π*x^2} es {@code 3*π}.
     */
    public Monomial constantPart() {
        Map<Expression, Expression> constant = new LinkedHashMap<>();
        factors.forEach((base, exponent) -> {
            if (!dependsOnVariable(base, exponent)) {
                constant.put(base, exponent);
            }
        });
        return new Monomial(coefficient, constant);
    }

    /**
     * Los factores que dependen de la variable, con coeficiente 1:
     * en {@code 3*π*x^2} es {@code x^2}.
     */
    public Monomial variablePart() {
        Map<Expression, Expression> variable = new LinkedHashMap<>();
        factors.forEach((base, exponent) -> {
            if (dependsOnVariable(base, exponent)) {
                variable.put(base, exponent);
            }
        });
        return new Monomial(Fraction.ONE, variable);
    }

    public boolean dependsOnVariable() {
        return factors.entrySet().stream().anyMatch(f -> dependsOnVariable(f.getKey(), f.getValue()));
    }

    /**
     * La expresión del término, escrita como en el resto de la calculadora.
     */
    public Expression toExpression() {
        return Normalizer.buildMonomial(coefficient, factors);
    }

    private static boolean dependsOnVariable(Expression base, Expression exponent) {
        return base.dependsOnVariable() || exponent.dependsOnVariable();
    }
}
