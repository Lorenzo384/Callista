package com.callista.model.calculus.integral.technique;

import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.exception.UnsupportedIntegralException;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Fraction;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.Monomial;
import com.callista.model.expression.Product;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Integración por partes: {@code ∫u dv = u·v - ∫v du}.
 *
 * <p>u se elige con la regla <b>ILATE</b>: el factor que aparece primero en
 * Inversas trigonométricas, Logarítmicas, Algebraicas, Trigonométricas,
 * Exponenciales; dv es el resto. Así se derivan los factores que se
 * simplifican al derivar ({@code ln x → 1/x}, {@code x² → 2x}) y se integran
 * los que se integran fácil ({@code eˣ}, {@code sen x}).</p>
 *
 * <p>Casos que resuelve:</p>
 * <ul>
 *     <li>Un producto: {@code ∫x·eˣ dx}, {@code ∫x·ln(x) dx}.</li>
 *     <li>Una sola función inversa o logarítmica, con dv = dx:
 *     {@code ∫ln(x) dx}, {@code ∫arctan(x) dx}.</li>
 *     <li>Varias veces: {@code ∫x²·eˣ dx} (la integral que queda vuelve a
 *     pasar por el integrador).</li>
 *     <li>Integrales cíclicas: en {@code ∫eˣ·sen(x) dx}, al aplicar partes
 *     dos veces reaparece la integral original y se despeja.</li>
 * </ul>
 */
public class IntegrationByParts implements IntegrationTechnique {

    /** Tipos de factor según la regla ILATE, en orden de preferencia para u. */
    enum Kind {
        INVERSE_TRIGONOMETRIC("inversa trigonométrica"),
        LOGARITHMIC("logarítmica"),
        ALGEBRAIC("algebraica"),
        TRIGONOMETRIC("trigonométrica"),
        EXPONENTIAL("exponencial");

        private final String description;

        Kind(String description) {
            this.description = description;
        }
    }

    /** Lo que deja un paso de partes: u·v y el integrando de ∫v du. */
    private record Parts(Expression boundary, Expression remaining) {
    }



    @Override
    public Optional<Expression> integrate(Monomial integrand, IntegrationContext context, int level) {
        Expression f = integrand.toExpression();
        int checkpoint = context.checkpoint();
        try {
            Optional<Parts> first = applyOnce(integrand, context, level);
            if (first.isEmpty()) {
                return Optional.empty();
            }
            Parts parts = first.get();

            // I = u·v - ∫v du, con ∫v du = k·I
            Optional<Expression> k = ratio(parts.remaining(), f);
            if (k.isPresent()) {
                return solveCycle(f, parts.boundary(), Simplifier.negate(k.get()), context, level);
            }
            Optional<Expression> twice = cycleAfterSecondStep(f, parts, context, level);
            if (twice.isPresent()) {
                return twice;
            }

            Expression rest = context.integrate(parts.remaining(), level + 1);
            Expression result = Simplifier.expand(new Difference(parts.boundary(), rest));
            context.record(level, "Resultado por partes", context.integral(f) + " = " + result);
            return Optional.of(result);
        } catch (UnsupportedIntegralException notThisWay) {
            context.rollback(checkpoint);
            return Optional.empty();
        }
    }

    /**
     * Un paso de partes: elige u y dv, calcula du y v (integrando dv) y
     * plantea {@code u·v - ∫v du}.
     *
     * @return vacío si la regla no aplica a esta forma
     */
    private Optional<Parts> applyOnce(Monomial integrand, IntegrationContext context, int level)
            throws UnsupportedIntegralException {
        Map<Expression, Expression> factors = integrand.factors();
        if (factors.isEmpty()) {
            return Optional.empty();
        }
        Kind best = factors.entrySet().stream()
                .map(factor -> kind(factor.getKey(), factor.getValue()))
                .min(Comparator.naturalOrder())
                .orElseThrow();

        Map<Expression, Expression> uFactors = new LinkedHashMap<>();
        Map<Expression, Expression> dvFactors = new LinkedHashMap<>();
        factors.forEach((base, exponent) ->
                (kind(base, exponent) == best ? uFactors : dvFactors).put(base, exponent));

        // u debe simplificarse al derivar: polinomios, ln o inversas con exponente
        // entero positivo. Con u = 1/x o 1/ln(x) cada paso empeoraría la integral.
        boolean simplifiesWhenDerived = uFactors.values().stream().allMatch(IntegrationByParts::isPositiveInteger);
        if (!simplifiesWhenDerived) {
            return Optional.empty();
        }

        // Sin nada para dv solo tiene sentido con ln o una inversa: ∫ln(x) dx con dv = dx
        boolean alone = dvFactors.isEmpty();
        if (alone && best != Kind.INVERSE_TRIGONOMETRIC && best != Kind.LOGARITHMIC) {
            return Optional.empty();
        }

        String x = context.variableName();
        DerivativeCalculator derivatives = new DerivativeCalculator(x);

        Expression f = integrand.toExpression();
        Expression u = new Monomial(integrand.coefficient(), uFactors).toExpression();
        Expression dv = alone ? new Constant(1) : new Monomial(Fraction.ONE, dvFactors).toExpression();
        Expression du = Simplifier.simplify(derivatives.derive(u));

        context.record(level, IntegrationRule.BY_PARTS, context.integral(f) + ": u = " + u + " (" + best.description
                + ", por ILATE),  dv = " + (alone ? "" : factor(dv) + " ") + "d" + x);
        Expression v = context.integrate(dv, level + 1);
        context.record(level, "Derivar u e integrar dv", "du = " + factor(du) + " d" + x + ",  v = " + v);

        Expression boundary = Simplifier.simplify(new Product(u, v));
        Expression remaining = Simplifier.simplify(new Product(v, du));
        context.record(level, "Aplicar ∫u dv = u·v - ∫v du",
                context.integral(f) + " = " + boundary + " - " + context.integral(remaining));
        return Optional.of(new Parts(boundary, remaining));
    }

    /**
     * Para integrales como {@code ∫eˣ·sen(x) dx}: si al aplicar partes a la
     * integral que quedó reaparece la original, se despeja. Si no, se
     * descartan los pasos de este intento.
     */
    private Optional<Expression> cycleAfterSecondStep(Expression f, Parts first, IntegrationContext context,
                                                      int level) throws UnsupportedIntegralException {
        List<Monomial> terms = Simplifier.terms(first.remaining());
        if (terms.size() != 1) {
            return Optional.empty();
        }
        Expression c = terms.getFirst()
                .constantPart(context.variableName())
                .toExpression();

        int checkpoint = context.checkpoint();

        Optional<Parts> second = applyOnce(
                terms.getFirst().variablePart(context.variableName()),
                context,
                level + 1);
        if (second.isPresent()) {
            // I = A₁ - c·(A₂ - ∫w₂) = A₁ - c·A₂ + c·∫w₂, con c·w₂ = k·I
            Optional<Expression> k = ratio(new Product(c, second.get().remaining()), f);
            if (k.isPresent()) {
                Expression boundary = Simplifier.simplify(
                        new Difference(first.boundary(), new Product(c, second.get().boundary())));
                return solveCycle(f, boundary, k.get(), context, level);
            }
        }
        context.rollback(checkpoint);
        return Optional.empty();
    }

    /**
     * Despeja la integral: si {@code I = A + m·I}, entonces
     * {@code I = A/(1 - m)}.
     */
    private Optional<Expression> solveCycle(Expression f, Expression a, Expression m, IntegrationContext context,
                                            int level) {
        Expression divisor = Simplifier.simplify(new Difference(new Constant(1), m));
        if (!divisor.dependsOnVariable() && divisor.evaluate(0) == 0) {
            return Optional.empty();
        }
        Expression result = Simplifier.expand(new Quotient(a, divisor));
        context.record(level, IntegrationRule.CYCLIC_INTEGRAL, "con I = " + context.integral(f) + ": I = " + a
                + multipleOfI(Simplifier.simplify(m)) + "  ⇒  I = " + result);
        return Optional.of(result);
    }

    /** {@code + 2·I}, {@code - I}, {@code - 3·I}. */
    private static String multipleOfI(Expression m) {
        boolean negative = !m.dependsOnVariable() && m.evaluate(0) < 0;
        Expression magnitude = negative ? Simplifier.simplify(Simplifier.negate(m)) : m;
        String coefficient = magnitude instanceof Constant c && c.isNumber(1) ? "" : factor(magnitude) + "·";
        return (negative ? " - " : " + ") + coefficient + "I";
    }

    /**
     * {@code w/f} si es una constante (la integral que quedó es un múltiplo
     * de la original).
     */
    private static Optional<Expression> ratio(Expression w, Expression f) {
        Expression r = Simplifier.simplify(new Quotient(w, f));
        return r.dependsOnVariable() ? Optional.empty() : Optional.of(r);
    }

    /**
     * Tipo ILATE de un factor {@code base^exponente}.
     */
    static Kind kind(Expression base, Expression exponent) {
        if (!base.dependsOnVariable() && exponent.dependsOnVariable()) {
            return Kind.EXPONENTIAL; // e^x, 2^x
        }
        if (base instanceof FunctionCall call) {
            return switch (call.getFunction()) {
                case ARCSIN, ARCCOS, ARCTAN -> Kind.INVERSE_TRIGONOMETRIC;
                case LN, LOG -> Kind.LOGARITHMIC;
                case SIN, COS, TAN, COT, SEC, CSC, SINH, COSH, TANH -> Kind.TRIGONOMETRIC;
                case EXP -> Kind.EXPONENTIAL;
                default -> Kind.ALGEBRAIC;
            };
        }
        return Kind.ALGEBRAIC; // x^n, sumas, raíces
    }

    private static boolean isPositiveInteger(Expression exponent) {
        return Simplifier.numericValue(exponent)
                .map(n -> n.isInteger() && n.signum() > 0)
                .orElse(false);
    }

    private static String factor(Expression e) {
        return e instanceof Sum || e instanceof Difference ? "(" + e + ")" : e.toString();
    }
}
