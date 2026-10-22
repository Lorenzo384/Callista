package com.callista.model.calculus.integral.technique;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Fraction;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.expression.Monomial;
import com.callista.model.expression.Negation;
import com.callista.model.expression.Power;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;

import java.util.Map;
import java.util.Optional;

/**
 * Integrales inmediatas: las de la tabla, cuando el argumento es
 * directamente la variable ({@code sin(x)}, {@code x^3}, {@code e^x},
 * {@code 1/(x^2 + 1)}…). Si el argumento es otra expresión
 * ({@code sin(3x)}), se resuelve con {@link SubstitutionMethod}.
 */
public class DirectIntegration implements IntegrationTechnique {

    @Override
    public Optional<Expression> integrate(Monomial integrand, IntegrationContext context, int level) {
        Variable x = new Variable(context.variableName());
        Map<Expression, Expression> factors = integrand.factors();
        Optional<Entry> entry = switch (factors.size()) {
            case 1 -> {
                Map.Entry<Expression, Expression> factor = factors.entrySet().iterator().next();
                yield single(factor.getKey(), factor.getValue(), x);
            }
            case 2 -> pair(factors, x);
            default -> Optional.empty();
        };
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        Expression result = Simplifier.simplify(entry.get().result());
        context.record(level, entry.get().title(), context.integral(integrand.toExpression()) + " = " + result);
        return Optional.of(result);
    }

    /** Una forma de la tabla: el nombre del paso y la antiderivada. */
    private record Entry(String title, Expression result) {
    }

    /**
     * Un solo factor {@code base^exponente}.
     */
    private Optional<Entry> single(Expression base, Expression exponent, Variable x) {
        Optional<Fraction> n = Simplifier.numericValue(exponent);

        // xⁿ y 1/x
        if (base.equals(x) && n.isPresent()) {
            if (n.get().equals(Fraction.of(-1))) {
                return entry(IntegrationRule.LOGARITHM.getDisplayName(), ln(abs(x)));
            }
            Expression next = n.get().add(Fraction.ONE).toExpression();
            return entry(IntegrationRule.POWER.getDisplayName(), new Quotient(new Power(x, next), next));
        }

        // aˣ y eˣ
        if (!base.dependsOnVariable() && exponent.equals(x)) {
            if (base.equals(Constant.E)) {
                return entry("Integral de la exponencial", new Power(Constant.E, x));
            }
            return entry("Integral de aˣ", new Quotient(new Power(base, x), ln(base)));
        }

        // Funciones de x: sin(x), sec(x)^2, 1/cos(x)…
        if (base instanceof FunctionCall call && call.getArgument().equals(x) && n.isPresent()
                && n.get().isInteger()) {
            return functionOfX(call.getFunction(), (int) n.get().getNumerator(), x);
        }

        // 1/(x^2 + 1) y 1/√(1 - x^2)
        if (base.equals(Simplifier.simplify(new Sum(new Power(x, two()), one()))) && n.equals(Optional.of(Fraction.of(-1)))) {
            return entry("Integral de 1/(1 + x²)", call(MathFunction.ARCTAN, x));
        }
        if (base.equals(Simplifier.simplify(new Difference(one(), new Power(x, two()))))
                && n.equals(Optional.of(Fraction.of(-1, 2)))) {
            return entry("Integral de 1/√(1 - x²)", call(MathFunction.ARCSIN, x));
        }
        return Optional.empty();
    }

    /**
     * La tabla para f(x)ⁿ: n = 1 las funciones básicas, n = 2 las que son
     * derivada de tan y cot, y las potencias negativas de sen y cos
     * ({@code 1/cos(x) = sec(x)}).
     */
    private Optional<Entry> functionOfX(MathFunction function, int n, Variable x) {
        return switch (function) {
            case SIN -> switch (n) {
                case 1 -> entry("Integral del seno", negate(call(MathFunction.COS, x)));
                case -1 -> cosecant(x);
                case -2 -> entry("Integral de csc²(x)", negate(call(MathFunction.COT, x)));
                default -> Optional.empty();
            };
            case COS -> switch (n) {
                case 1 -> entry("Integral del coseno", call(MathFunction.SIN, x));
                case -1 -> secant(x);
                case -2 -> entry("Integral de sec²(x)", call(MathFunction.TAN, x));
                default -> Optional.empty();
            };
            case TAN -> n == 1
                    ? entry("Integral de la tangente", negate(ln(abs(call(MathFunction.COS, x)))))
                    : Optional.empty();
            case COT -> n == 1
                    ? entry("Integral de la cotangente", ln(abs(call(MathFunction.SIN, x))))
                    : Optional.empty();
            case SEC -> switch (n) {
                case 1 -> secant(x);
                case 2 -> entry("Integral de sec²(x)", call(MathFunction.TAN, x));
                default -> Optional.empty();
            };
            case CSC -> switch (n) {
                case 1 -> cosecant(x);
                case 2 -> entry("Integral de csc²(x)", negate(call(MathFunction.COT, x)));
                default -> Optional.empty();
            };
            case EXP -> n == 1 ? entry("Integral de la exponencial", call(MathFunction.EXP, x)) : Optional.empty();
            case SINH -> n == 1 ? entry("Integral del seno hiperbólico", call(MathFunction.COSH, x)) : Optional.empty();
            case COSH -> n == 1 ? entry("Integral del coseno hiperbólico", call(MathFunction.SINH, x)) : Optional.empty();
            case TANH -> n == 1
                    ? entry("Integral de la tangente hiperbólica", ln(call(MathFunction.COSH, x)))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * Dos factores que juntos son una derivada conocida:
     * {@code sec(x)·tan(x)} y {@code csc(x)·cot(x)}, también escritos como
     * {@code sin(x)/cos(x)^2} y {@code cos(x)/sin(x)^2}.
     */
    private Optional<Entry> pair(Map<Expression, Expression> factors, Variable x) {
        if (hasFactors(factors, x, MathFunction.SEC, 1, MathFunction.TAN, 1)
                || hasFactors(factors, x, MathFunction.SIN, 1, MathFunction.COS, -2)) {
            return entry("Integral de sec(x)·tan(x)", call(MathFunction.SEC, x));
        }
        if (hasFactors(factors, x, MathFunction.CSC, 1, MathFunction.COT, 1)
                || hasFactors(factors, x, MathFunction.COS, 1, MathFunction.SIN, -2)) {
            return entry("Integral de csc(x)·cot(x)", negate(call(MathFunction.CSC, x)));
        }
        return Optional.empty();
    }

    private static boolean hasFactors(Map<Expression, Expression> factors, Variable x,
                                      MathFunction f, int fExponent, MathFunction g, int gExponent) {
        return Fraction.of(fExponent).toExpression().equals(factors.get(call(f, x)))
                && Fraction.of(gExponent).toExpression().equals(factors.get(call(g, x)));
    }

    private static Optional<Entry> secant(Variable x) {
        return entry("Integral de la secante",
                ln(abs(new Sum(call(MathFunction.SEC, x), call(MathFunction.TAN, x)))));
    }

    private static Optional<Entry> cosecant(Variable x) {
        return entry("Integral de la cosecante",
                negate(ln(abs(new Sum(call(MathFunction.CSC, x), call(MathFunction.COT, x))))));
    }

    // ------------------------------------------------------------ apoyo

    private static Optional<Entry> entry(String title, Expression result) {
        return Optional.of(new Entry(title, result));
    }

    private static FunctionCall call(MathFunction function, Expression argument) {
        return new FunctionCall(function, argument);
    }

    private static Expression ln(Expression argument) {
        return call(MathFunction.LN, argument);
    }

    private static Expression abs(Expression argument) {
        return call(MathFunction.ABS, argument);
    }

    private static Expression negate(Expression expression) {
        return new Negation(expression);
    }

    private static Constant one() {
        return new Constant(1);
    }

    private static Constant two() {
        return new Constant(2);
    }
}
