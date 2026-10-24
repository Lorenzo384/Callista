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
     * Parte constante usando el criterio original:
     * cualquier factor con variables se considera variable.
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
     * Parte variable usando el criterio original.
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

    /**
     * Parte constante respecto de una variable específica.
     */
    public Monomial constantPart(String variableName) {
        Map<Expression, Expression> constant = new LinkedHashMap<>();

        factors.forEach((base, exponent) -> {
            if (!base.dependsOn(variableName)
                    && !exponent.dependsOn(variableName)) {
                constant.put(base, exponent);
            }
        });

        return new Monomial(coefficient, constant);
    }

    /**
     * Parte que depende de la variable de integración seleccionada.
     */
    public Monomial variablePart(String variableName) {
        Map<Expression, Expression> variable = new LinkedHashMap<>();

        factors.forEach((base, exponent) -> {
            if (base.dependsOn(variableName)
                    || exponent.dependsOn(variableName)) {
                variable.put(base, exponent);
            }
        });

        return new Monomial(Fraction.ONE, variable);
    }

    public boolean dependsOnVariable() {
        return factors.entrySet().stream()
                .anyMatch(f -> dependsOnVariable(f.getKey(), f.getValue()));
    }

    public boolean dependsOnVariable(String variableName) {
        return factors.entrySet().stream()
                .anyMatch(f ->
                        f.getKey().dependsOn(variableName)
                                || f.getValue().dependsOn(variableName));
    }

    public Expression toExpression() {
        return Normalizer.buildMonomial(coefficient, factors);
    }

    private static boolean dependsOnVariable(
            Expression base, Expression exponent) {
        return base.dependsOnVariable() || exponent.dependsOnVariable();
    }
}
