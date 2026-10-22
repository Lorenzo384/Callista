package com.callista.model.calculus.integral.technique;

import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Expression;

/**
 * Lo que una técnica de integración necesita del integrador: integrar
 * otras expresiones (recursión), registrar pasos y deshacerlos si la
 * técnica no funciona.
 */
public interface IntegrationContext {

    /**
     * Nombre de la variable de integración: {@code x}, o {@code u} dentro de
     * una sustitución.
     */
    String variableName();

    /**
     * Integra otra expresión con la misma variable (por ejemplo, cada término
     * de una suma).
     */
    Expression integrate(Expression integrand, int level) throws UnsupportedIntegralException;

    /**
     * Integra una expresión en otra variable: la integral en u de una
     * sustitución.
     */
    Expression integrate(Expression integrand, String variable, int level) throws UnsupportedIntegralException;

    /**
     * Registra un paso del procedimiento.
     */
    void record(int level, String title, String statement);

    default void record(int level, IntegrationRule rule, String statement) {
        record(level, rule.getDisplayName(), statement);
    }

    /**
     * Marca la posición actual de los pasos, para deshacerlos con
     * {@link #rollback(int)} si un intento no funciona.
     */
    int checkpoint();

    /**
     * Descarta los pasos registrados después de {@code checkpoint}.
     */
    void rollback(int checkpoint);

    /**
     * Cómo se escribe la integral: {@code ∫x*cos(x^2) dx}.
     */
    default String integral(Expression integrand) {
        String text = integrand.precedence() < Expression.MULTIPLICATIVE ? "(" + integrand + ")" : integrand.toString();
        return "∫" + text + " d" + variableName();
    }
}
