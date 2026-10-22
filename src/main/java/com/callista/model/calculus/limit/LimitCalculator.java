package com.callista.model.calculus.limit;

import com.callista.model.calculus.area.CurveIntersections;
import com.callista.model.calculus.derivative.DerivativeCalculator;
import com.callista.model.entity.SolutionStep;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.FunctionCall;
import com.callista.model.expression.MathFunction;
import com.callista.model.expression.Negation;
import com.callista.model.expression.Power;
import com.callista.model.expression.Product;
import com.callista.model.expression.Quotient;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;
import com.callista.util.FormatUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Límite de una función de una variable cuando tiende a ∞, a -∞ o a un
 * número por un lado.
 *
 * <p>Primero se intenta con <b>reglas</b>, recorriendo el árbol:</p>
 * <ul>
 *     <li>sustitución directa si la función es continua en el punto;</li>
 *     <li>límites conocidos: {@code 1/t → 0}, {@code e^(-t) → 0},
 *     {@code arctan t → π/2}, {@code ln t → ∞}, {@code sen t} no tiene límite…;</li>
 *     <li>álgebra de límites (suma, producto, cociente, potencia), con
 *     ∞ + ∞ = ∞, k/∞ = 0, k/0⁺ = ∞…;</li>
 *     <li>regla de <b>L'Hôpital</b> para 0/0 e ∞/∞ (y 0·∞ escrito como cociente).</li>
 * </ul>
 *
 * <p>Si las reglas no alcanzan (∞ − ∞, oscilaciones combinadas…), se
 * <b>estima</b> con una tabla de valores cada vez más cerca del destino.</p>
 */
public class LimitCalculator {

    /** Un valor mayor que este se toma como infinito: tan(π/2) da 1.6e16 por redondeo. */
    private static final double HUGE = 1e12;
    /** Veces que se puede aplicar L'Hôpital en un mismo límite. */
    private static final int MAX_LHOPITAL = 4;
    /** Qué tan cerca se evalúa para comprobar o estimar: 10^k o c ± 10^-k. */
    private static final int[] TABLE = {1, 2, 3, 4, 6, 8};

    /**
     * Calcula el límite de {@code f} cuando {@code variable} tiende a
     * {@code target}, agregando a {@code steps} las reglas que se usaron o la
     * tabla de valores.
     */
    public Limit limit(Expression f, Variable variable, LimitPoint target, List<SolutionStep> steps, int level) {
        Evaluation evaluation = new Evaluation(variable, target);
        Optional<Limit> symbolic = evaluation.eval(f);
        if (symbolic.isPresent() && agreesNumerically(f, symbolic.get(), target)) {
            evaluation.notes.forEach(note -> steps.add(new SolutionStep(note, level)));
            return symbolic.get();
        }
        return numericLimit(f, variable, target, steps, level);
    }

    /**
     * Red de seguridad: el resultado de las reglas debe parecerse a lo que
     * da la función muy cerca del destino.
     */
    private static boolean agreesNumerically(Expression f, Limit limit, LimitPoint target) {
        double far = f.evaluate(target.near(4));
        double near = f.evaluate(target.near(8));
        if (!Double.isFinite(near) || !Double.isFinite(far)) {
            return true;
        }
        return switch (limit.kind()) {
            case FINITE -> {
                double value = limit.approximation();
                yield Math.abs(near - value) <= 0.05 * (1 + Math.abs(value));
            }
            case POSITIVE_INFINITY -> near > 0 && near >= far;
            case NEGATIVE_INFINITY -> near < 0 && near <= far;
            case NONE -> true;
        };
    }

    /**
     * Estimación con una tabla de valores: si se estabilizan, el límite es
     * ese valor; si crecen sin parar, es ±∞; si no, no existe.
     */
    private static Limit numericLimit(Expression f, Variable variable, LimitPoint target,
                                      List<SolutionStep> steps, int level) {
        steps.add(new SolutionStep("Las reglas no alcanzan: se estima el límite con una tabla de valores "
                + "cada vez más cerca (" + target.describe(variable.toString()) + "):", level));
        double[] values = new double[TABLE.length];
        for (int i = 0; i < TABLE.length; i++) {
            double t = target.near(TABLE[i]);
            values[i] = f.evaluate(t);
            steps.add(new SolutionStep(variable + " = " + target.nearText(TABLE[i]) + "   →   "
                    + FormatUtils.formatNumber(values[i]), level + 1));
        }
        int n = values.length;
        double last = values[n - 1];
        double before = values[n - 2];
        double earlier = values[n - 3];
        Limit result;
        if (Double.isFinite(last) && Double.isFinite(before) && Double.isFinite(earlier)) {
            double recent = Math.abs(last - before);
            double previous = Math.abs(before - earlier);
            boolean settles = recent < 1e-6 * (1 + Math.abs(last))
                    || (recent < previous / 10 && recent < 1e-3 * (1 + Math.abs(last)));
            boolean grows = Math.abs(last) > Math.abs(before) && Math.abs(before) > Math.abs(earlier)
                    && Math.signum(last) == Math.signum(before) && recent >= previous / 2;
            if (settles) {
                result = estimate(last, recent);
            } else if (grows) {
                result = Limit.infinity(last);
            } else {
                result = Limit.NONE;
            }
        } else {
            result = Limit.NONE;
        }
        String conclusion = switch (result.kind()) {
            case FINITE -> "Los valores se acercan a " + result.value() + ": el límite es " + result;
            case POSITIVE_INFINITY, NEGATIVE_INFINITY -> "Los valores crecen sin límite: el límite es " + result;
            case NONE -> "Los valores no se acercan a ningún número: el límite no existe";
        };
        steps.add(new SolutionStep(conclusion, level));
        return result;
    }

    /** El valor estimado, reconocido como valor sencillo si se puede (0, 1/2, π/2). */
    private static Limit estimate(double value, double uncertainty) {
        if (Math.abs(value) < Math.max(1e-6, uncertainty)) {
            return new Limit(Limit.Kind.FINITE, new Constant(0), false);
        }
        if (uncertainty < 1e-9) {
            Optional<Expression> exact = CurveIntersections.exactValue(value);
            if (exact.isPresent()) {
                return new Limit(Limit.Kind.FINITE, exact.get(), false);
            }
        }
        return Limit.approximate(value);
    }

    /**
     * Un cálculo de límite: recorre el árbol aplicando las reglas y guarda
     * una nota de cada límite conocido o regla usada.
     */
    private static final class Evaluation {

        private final Variable variable;
        private final LimitPoint target;
        private final DerivativeCalculator derivatives;
        private final Set<String> notes = new LinkedHashSet<>();
        private int lhopitalCount;

        Evaluation(Variable variable, LimitPoint target) {
            this.variable = variable;
            this.target = target;
            this.derivatives = new DerivativeCalculator(variable.toString());
        }

        /** El límite de e, o vacío si las reglas no lo deciden. */
        Optional<Limit> eval(Expression e) {
            if (!e.dependsOnVariable()) {
                return Optional.of(Limit.finite(e));
            }
            if (!target.isInfinite()) {
                Optional<Limit> substituted = substitute(e);
                if (substituted.isPresent()) {
                    return substituted;
                }
            }
            return switch (e) {
                case Variable v -> Optional.of(target.isInfinite()
                        ? Limit.infinity(target.kind() == LimitPoint.Kind.POSITIVE_INFINITY ? 1 : -1)
                        : Limit.finite(target.point()));
                case Negation n -> eval(n.getOperand()).map(Evaluation::negate);
                case Sum s -> add(eval(s.getLeft()), eval(s.getRight()).map(l -> l));
                case Difference d -> add(eval(d.getLeft()), eval(d.getRight()).map(Evaluation::negate));
                case Product p -> product(p.getLeft(), p.getRight());
                case Quotient q -> quotient(q.getLeft(), q.getRight());
                case Power p -> power(p);
                case FunctionCall f -> function(f);
                default -> Optional.empty();
            };
        }

        /**
         * Sustitución directa: si e está definida en c y es continua ahí
         * (vale casi lo mismo muy cerca), el límite es e(c).
         */
        private Optional<Limit> substitute(Expression e) {
            double c = target.point().evaluate(0);
            double at = e.evaluate(c);
            double near = e.evaluate(target.near(9));
            if (Double.isFinite(at) && Double.isFinite(near) && Math.abs(near - at) < 1e-4 * (1 + Math.abs(at))) {
                return Optional.of(Limit.finite(Simplifier.simplify(e.replace(variable, target.point()))));
            }
            return Optional.empty();
        }

        private static Limit negate(Limit l) {
            return switch (l.kind()) {
                case FINITE -> new Limit(Limit.Kind.FINITE, Simplifier.simplify(new Negation(l.value())), l.exact());
                case POSITIVE_INFINITY -> Limit.NEGATIVE_INFINITY;
                case NEGATIVE_INFINITY -> Limit.POSITIVE_INFINITY;
                case NONE -> Limit.NONE;
            };
        }

        /** Suma: ∞ + k = ∞; ∞ + ∞ = ∞; ∞ − ∞ no se decide aquí. */
        private Optional<Limit> add(Optional<Limit> left, Optional<Limit> right) {
            if (left.isEmpty() || right.isEmpty()) {
                return Optional.empty();
            }
            Limit a = left.get();
            Limit b = right.get();
            if (a.isFinite() && b.isFinite()) {
                return Optional.of(new Limit(Limit.Kind.FINITE, Simplifier.simplify(new Sum(a.value(), b.value())),
                        a.exact() && b.exact()));
            }
            if (a.isInfinite() && b.isInfinite()) {
                return a.kind() == b.kind() ? Optional.of(a) : Optional.empty();
            }
            if (a.isInfinite() || b.isInfinite()) {
                return Optional.of(a.isInfinite() ? a : b); // ∞ + algo acotado
            }
            if (a.kind() == Limit.Kind.NONE && b.kind() == Limit.Kind.NONE) {
                return Optional.empty();
            }
            return Optional.of(Limit.NONE);
        }

        /** Producto: k·∞ = ±∞; 0·∞ se reescribe como cociente para L'Hôpital. */
        private Optional<Limit> product(Expression left, Expression right) {
            Optional<Limit> l = eval(left);
            Optional<Limit> r = eval(right);
            if (l.isEmpty() || r.isEmpty()) {
                return Optional.empty();
            }
            Limit a = l.get();
            Limit b = r.get();
            if (a.isFinite() && b.isFinite()) {
                return Optional.of(new Limit(Limit.Kind.FINITE,
                        Simplifier.simplify(new Product(a.value(), b.value())), a.exact() && b.exact()));
            }
            if ((a.isZero() && b.isInfinite()) || (a.isInfinite() && b.isZero())) {
                Expression zero = a.isZero() ? left : right;
                Expression infinite = a.isZero() ? right : left;
                Expression reciprocal = Simplifier.simplify(new Quotient(new Constant(1), zero));
                notes.add("Indeterminación 0·∞: " + simplified(new Product(zero, infinite)) + " = "
                        + wrap(infinite) + "/" + wrap(reciprocal) + ", que es ∞/∞");
                return lhopital(infinite, reciprocal, "∞/∞");
            }
            if (a.isInfinite() && b.isInfinite()) {
                return Optional.of(Limit.infinity(sign(a) * sign(b)));
            }
            if (a.isInfinite() || b.isInfinite()) {
                Limit finite = a.isInfinite() ? b : a;
                if (!finite.isFinite()) {
                    return Optional.empty(); // ∞ por algo que oscila
                }
                return Optional.of(Limit.infinity(sign(a.isInfinite() ? a : b) * sign(finite)));
            }
            // Uno no tiene límite: por un número distinto de 0 sigue sin tenerlo
            Limit other = a.kind() == Limit.Kind.NONE ? b : a;
            return other.isFinite() && !other.isZero() ? Optional.of(Limit.NONE) : Optional.empty();
        }

        /** Cociente: k/∞ = 0, k/0 = ±∞, 0/0 e ∞/∞ con L'Hôpital. */
        private Optional<Limit> quotient(Expression numerator, Expression denominator) {
            Optional<Limit> n = eval(numerator);
            Optional<Limit> d = eval(denominator);
            if (n.isEmpty() || d.isEmpty()) {
                return Optional.empty();
            }
            Limit a = n.get();
            Limit b = d.get();
            Expression whole = new Quotient(numerator, denominator);
            if (b.isFinite() && !b.isZero()) {
                return switch (a.kind()) {
                    case FINITE -> Optional.of(new Limit(Limit.Kind.FINITE,
                            Simplifier.simplify(new Quotient(a.value(), b.value())), a.exact() && b.exact()));
                    case POSITIVE_INFINITY, NEGATIVE_INFINITY -> Optional.of(Limit.infinity(sign(a) * sign(b)));
                    case NONE -> Optional.of(Limit.NONE);
                };
            }
            if (b.isZero()) {
                if (a.isZero()) {
                    return lhopital(numerator, denominator, "0/0");
                }
                if (a.kind() == Limit.Kind.NONE) {
                    return Optional.empty();
                }
                Limit result = Limit.infinity(signNear(whole));
                notes.add(simplified(whole) + " → " + result + " porque el denominador " + denominator + " → 0");
                return Optional.of(result);
            }
            if (b.isInfinite()) {
                if (a.isInfinite()) {
                    return lhopital(numerator, denominator, "∞/∞");
                }
                if (a.isFinite() || a.kind() == Limit.Kind.NONE) {
                    notes.add(simplified(whole) + " → 0 porque el denominador " + denominator + " → " + b);
                    return Optional.of(Limit.finite(new Constant(0)));
                }
            }
            return Optional.empty();
        }

        /**
         * Regla de L'Hôpital: lim N/D = lim N'/D' cuando da 0/0 o ∞/∞.
         */
        private Optional<Limit> lhopital(Expression numerator, Expression denominator, String form) {
            if (lhopitalCount >= MAX_LHOPITAL) {
                return Optional.empty();
            }
            lhopitalCount++;
            Expression dn = Simplifier.simplify(derivatives.derive(numerator));
            Expression dd = Simplifier.simplify(derivatives.derive(denominator));
            Expression ratio = Simplifier.simplify(new Quotient(dn, dd));
            notes.add("Indeterminación " + form + " en " + simplified(new Quotient(numerator, denominator))
                    + ": regla de L'Hôpital, se derivan numerador y denominador: " + wrap(dn) + "/" + wrap(dd)
                    + (ratio.equals(new Quotient(dn, dd)) ? "" : " = " + ratio));
            return eval(ratio);
        }

        /** Potencias: t^n, a^t y, si ambos varían, e^(exponente·ln base). */
        private Optional<Limit> power(Power p) {
            Expression base = p.getLeft();
            Expression exponent = p.getRight();
            if (!exponent.dependsOnVariable()) {
                double n = exponent.evaluate(0);
                Optional<Limit> l = eval(base);
                if (l.isEmpty()) {
                    return Optional.empty();
                }
                Limit b = l.get();
                if (b.isFinite()) {
                    double value = b.approximation();
                    if (value == 0 && n < 0) {
                        Limit result = Limit.infinity(signNear(p));
                        notes.add(simplified(p) + " → " + result + " porque " + base + " → 0");
                        return Optional.of(result);
                    }
                    if (value > 0 || n == Math.rint(n) || value == 0) {
                        return Optional.of(new Limit(Limit.Kind.FINITE,
                                Simplifier.simplify(new Power(b.value(), exponent)), b.exact()));
                    }
                    return Optional.empty();
                }
                if (b.isInfinite()) {
                    if (n > 0) {
                        return Optional.of(Limit.infinity(signNear(p)));
                    }
                    if (n < 0) {
                        notes.add(simplified(p) + " → 0 porque " + base + " → " + b);
                        return Optional.of(Limit.finite(new Constant(0)));
                    }
                    return Optional.of(Limit.finite(new Constant(1)));
                }
                return Optional.empty();
            }
            if (!base.dependsOnVariable()) {
                double a = base.evaluate(0);
                Optional<Limit> l = eval(exponent);
                if (l.isEmpty() || a <= 0) {
                    return Optional.empty();
                }
                Limit x = l.get();
                Limit result = switch (x.kind()) {
                    case FINITE -> new Limit(Limit.Kind.FINITE, Simplifier.simplify(new Power(base, x.value())),
                            x.exact());
                    case POSITIVE_INFINITY -> a > 1 ? Limit.POSITIVE_INFINITY
                            : a < 1 ? Limit.finite(new Constant(0)) : Limit.finite(new Constant(1));
                    case NEGATIVE_INFINITY -> a > 1 ? Limit.finite(new Constant(0))
                            : a < 1 ? Limit.POSITIVE_INFINITY : Limit.finite(new Constant(1));
                    case NONE -> null;
                };
                if (result == null) {
                    return Optional.empty();
                }
                if (x.isInfinite()) {
                    notes.add(simplified(p) + " → " + result + because(exponent, x));
                }
                return Optional.of(result);
            }
            // f(t)^g(t) = e^(g(t)·ln f(t))
            return eval(new Power(Constant.E, new Product(exponent, new FunctionCall(MathFunction.LN, base))));
        }

        /** Funciones elementales: continuas donde están definidas, y sus límites conocidos en ±∞. */
        private Optional<Limit> function(FunctionCall call) {
            MathFunction function = call.getFunction();
            Expression argument = call.getArgument();
            Optional<Limit> l = eval(argument);
            if (l.isEmpty()) {
                return Optional.empty();
            }
            Limit x = l.get();
            Limit result = switch (x.kind()) {
                case FINITE -> {
                    Expression value = new FunctionCall(function, x.value());
                    double exact = value.evaluate(0);
                    if (Double.isFinite(exact) && Math.abs(exact) < HUGE) {
                        yield new Limit(Limit.Kind.FINITE, Simplifier.simplify(value), x.exact());
                    }
                    // Fuera del dominio en el punto: ln(t) cuando t → 0⁺, tan(t) cuando t → π/2⁻
                    double closer = call.evaluate(target.near(8));
                    double farther = call.evaluate(target.near(6));
                    boolean blowsUp = Double.isFinite(closer) && Double.isFinite(farther)
                            && Math.abs(closer) > Math.abs(farther) && Math.signum(closer) == Math.signum(farther);
                    yield blowsUp ? Limit.infinity(closer) : null;
                }
                case POSITIVE_INFINITY -> switch (function) {
                    case EXP, LN, LOG, SQRT, SINH, COSH, ABS -> Limit.POSITIVE_INFINITY;
                    case ARCTAN -> Limit.finite(Simplifier.simplify(new Quotient(Constant.PI, new Constant(2))));
                    case TANH -> Limit.finite(new Constant(1));
                    case SIN, COS -> Limit.NONE;
                    default -> null;
                };
                case NEGATIVE_INFINITY -> switch (function) {
                    case EXP -> Limit.finite(new Constant(0));
                    case ARCTAN -> Limit.finite(Simplifier.simplify(
                            new Negation(new Quotient(Constant.PI, new Constant(2)))));
                    case TANH -> Limit.finite(new Constant(-1));
                    case SINH -> Limit.NEGATIVE_INFINITY;
                    case COSH, ABS -> Limit.POSITIVE_INFINITY;
                    case SIN, COS -> Limit.NONE;
                    default -> null;
                };
                case NONE -> null;
            };
            if (result == null) {
                return Optional.empty();
            }
            if (result.kind() == Limit.Kind.NONE) {
                notes.add(call + " no tiene límite (oscila entre -1 y 1)" + because(argument, x));
            } else if (x.isInfinite() || !result.isFinite()) {
                notes.add(call + " → " + result + because(argument, x));
            }
            return Optional.of(result);
        }

        /** {@code  cuando t → ∞} o {@code  porque -t → -∞}. */
        private String because(Expression argument, Limit argumentLimit) {
            if (argument.equals(variable)) {
                return " cuando " + target.describe(variable.toString());
            }
            return " porque " + argument + " → " + argumentLimit;
        }

        /** Signo de e muy cerca del destino. */
        private double signNear(Expression e) {
            return Math.signum(e.evaluate(target.near(8)));
        }

        private static double sign(Limit l) {
            return Math.signum(l.approximation());
        }

        private static Expression simplified(Expression e) {
            return Simplifier.simplify(e);
        }

        /** Entre paréntesis si es una suma, resta, producto o cociente. */
        private static String wrap(Expression e) {
            boolean compound = e instanceof Sum || e instanceof Difference || e instanceof Product
                    || e instanceof Quotient || e instanceof Negation;
            return compound ? "(" + e + ")" : e.toString();
        }
    }
}
