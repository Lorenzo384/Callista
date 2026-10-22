package com.callista.model.calculus.integral.technique;

import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Monomial;
import com.callista.model.expression.Simplifier;

import java.util.Optional;

/**
 * Desarrollar el producto: {@code ∫x(x + 1) dx = ∫(x^2 + x) dx} y
 * {@code ∫(x^2 + 1)/x dx = ∫(x + 1/x) dx}. Convierte el integrando en una
 * suma de términos que luego se integran uno por uno.
 */
public class AlgebraicExpansion implements IntegrationTechnique {

    @Override
    public Optional<Expression> integrate(Monomial integrand, IntegrationContext context, int level)
            throws UnsupportedIntegralException {
        Expression f = integrand.toExpression();
        Expression expanded = Simplifier.expand(f);
        if (expanded.equals(f) || Simplifier.terms(expanded).size() < 2) {
            return Optional.empty();
        }
        context.record(level, IntegrationRule.EXPANSION, context.integral(f) + " = " + context.integral(expanded));
        return Optional.of(context.integrate(expanded, level + 1));
    }
}
