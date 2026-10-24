package com.callista.model.calculus.integral.technique;

import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.BinaryOperation;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Fraction;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.Monomial;
import com.callista.model.expression.Negation;
import com.callista.model.expression.Power;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Sustitución (cambio de variable): {@code ∫f(g(x))·g'(x) dx = ∫f(u) du},
 * con {@code u = g(x)}.
 *
 * <p>Prueba como u cada «parte interior» del integrando: argumentos de
 * funciones ({@code x^2} en {@code cos(x^2)}), bases de potencias
 * ({@code x^2 + 1} en {@code (x^2 + 1)^5}), exponenciales y sus exponentes
 * ({@code e^x}, {@code 3x} en {@code e^(3x)}) y las propias funciones
 * ({@code ln(x)} en {@code ln(x)/x}). Para cada una calcula
 * {@code du = u'·dx} y divide el integrando entre {@code u'}; si al cambiar u
 * por la nueva variable ya no queda ninguna x, la sustitución funciona.</p>
 *
 * <p>Con una exponencial como u, sus potencias también se reescriben:
 * con {@code u = e^x}, {@code e^(2x)} es {@code u^2}.</p>
 */
public class SubstitutionMethod implements IntegrationTechnique {


    @Override
    public Optional<Expression> integrate(Monomial integrand, IntegrationContext context, int level) {
        Expression f = integrand.toExpression();
        String variable = context.variableName();

        DerivativeCalculator derivatives = new DerivativeCalculator(variable);

        Variable x = new Variable(variable);
        Variable u = new Variable(variable.equals("u") ? "w" : "u");

        for (Expression candidate : candidates(f, x)) {
            Expression du = Simplifier.simplify(derivatives.derive(candidate));
            if (!du.dependsOnVariable() && du.evaluate(0) == 0) {
                continue;
            }
            Expression quotient = Simplifier.simplify(new Quotient(f, du));
            Expression inU = Simplifier.simplify(rewritePowersOf(quotient, candidate, u).replace(candidate, u));
            if (inU.contains(x)) {
                continue;
            }

            int checkpoint = context.checkpoint();
            try {
                context.record(level, IntegrationRule.SUBSTITUTION, context.integral(f) + ": " + u + " = " + candidate
                        + ",  d" + u + " = " + factor(du) + " d" + x);
                context.record(level, "La integral queda", "∫" + factor(inU) + " d" + u);
                Expression inner = context.integrate(inU, u.getName(), level + 1);
                Expression result = Simplifier.simplify(inner.replace(u, candidate));
                context.record(level, IntegrationRule.BACK_SUBSTITUTION,
                        u + " = " + candidate + ": " + context.integral(f) + " = " + result);
                return Optional.of(result);
            } catch (UnsupportedIntegralException tryNext) {
                context.rollback(checkpoint);
            }
        }
        return Optional.empty();
    }

    /**
     * Partes interiores del integrando que pueden servir como u, de afuera
     * hacia adentro y sin repetir.
     */
    private static List<Expression> candidates(Expression f, Variable x) {
        Set<Expression> found = new LinkedHashSet<>();
        collect(f, found);
        List<Expression> candidates = new ArrayList<>();
        for (Expression candidate : found) {
            if (!candidate.equals(x) && !candidate.equals(f) && candidate.dependsOnVariable()) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private static void collect(Expression e, Set<Expression> found) {
        switch (e) {
            case FunctionCall call -> {
                found.add(call);
                found.add(call.getArgument());
                collect(call.getArgument(), found);
            }
            case Power power -> {
                if (!(power.getLeft() instanceof Variable)) {
                    found.add(power.getLeft());
                }
                if (power.getRight().dependsOnVariable()) {
                    found.add(power);           // la exponencial completa: e^x
                    found.add(power.getRight()); // su exponente: 3x en e^(3x)
                }
                collect(power.getLeft(), found);
                collect(power.getRight(), found);
            }
            case Sum sum -> {
                found.add(sum);
                collectOperands(sum, found);
            }
            case Difference difference -> {
                found.add(difference);
                collectOperands(difference, found);
            }
            case BinaryOperation operation -> collectOperands(operation, found);
            case Negation negation -> collect(negation.getOperand(), found);
            default -> {
                // Números y variables no tienen partes interiores
            }
        }
    }

    private static void collectOperands(BinaryOperation operation, Set<Expression> found) {
        collect(operation.getLeft(), found);
        collect(operation.getRight(), found);
    }

    /**
     * Si u es una exponencial {@code a^g(x)}, cada {@code a^(k·g(x))} se
     * escribe como {@code u^k}: con u = e^x, {@code e^(2x)} es {@code u^2}.
     */
    private static Expression rewritePowersOf(Expression e, Expression candidate, Variable u) {
        if (!(candidate instanceof Power exponential) || exponential.getLeft().dependsOnVariable()) {
            return e;
        }
        return rewrite(e, exponential.getLeft(), exponential.getRight(), u);
    }

    private static Expression rewrite(Expression e, Expression base, Expression exponent, Variable u) {
        if (e instanceof Power power && power.getLeft().equals(base)) {
            Optional<Fraction> k = Simplifier.numericValue(
                    Simplifier.simplify(new Quotient(power.getRight(), exponent)));
            if (k.isPresent()) {
                return new Power(u, k.get().toExpression());
            }
        }
        return switch (e) {
            case BinaryOperation operation -> operation.withOperands(
                    rewrite(operation.getLeft(), base, exponent, u), rewrite(operation.getRight(), base, exponent, u));
            case Negation negation -> new Negation(rewrite(negation.getOperand(), base, exponent, u));
            case FunctionCall call -> new FunctionCall(call.getFunction(), rewrite(call.getArgument(), base, exponent, u));
            default -> e;
        };
    }

    /** Con paréntesis si es suma o resta, para escribir {@code (2*x + 1) dx}. */
    private static String factor(Expression e) {
        return e.precedence() < Expression.MULTIPLICATIVE ? "(" + e + ")" : e.toString();
    }
}
