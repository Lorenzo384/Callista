package com.callista.model.calculus.derivative;

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
import com.callista.model.expression.Sum;
import com.callista.model.expression.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.callista.model.expression.Simplifier.difference;
import static com.callista.model.expression.Simplifier.negate;
import static com.callista.model.expression.Simplifier.power;
import static com.callista.model.expression.Simplifier.product;
import static com.callista.model.expression.Simplifier.quotient;
import static com.callista.model.expression.Simplifier.sum;

/**
 * Deriva expresiones y explica cada regla aplicada.
 *
 * <p>Recorre el árbol de arriba hacia abajo: en cada nodo identifica la regla
 * que corresponde (suma, producto, cadena…), registra un paso con lo que
 * plantea esa regla y deriva los hijos, cuyos pasos quedan un nivel más
 * adentro. Por ejemplo, para {@code 3x^2 + sen(x)}:</p>
 *
 * <pre>
 * Regla de la suma: d/dx[3*x^2 + sin(x)] = d/dx[3*x^2] + d/dx[sin(x)]
 *     Regla del múltiplo constante: d/dx[3*x^2] = 3*d/dx[x^2]
 *         Regla de la potencia: d/dx[x^2] = 2*x
 *     Derivada del seno: d/dx[sin(x)] = cos(x)
 * </pre>
 *
 * <p>Las reglas están aquí y no en cada nodo del árbol: el árbol solo
 * representa la expresión, y las operaciones de cálculo (derivar, integrar)
 * se agregan sin modificarlo.</p>
 */
public class DerivativeCalculator {

    private static final Constant ZERO = new Constant(0);
    private static final Constant ONE = new Constant(1);
    private static final Constant TWO = new Constant(2);

    private final String variableName;

    public DerivativeCalculator() {
        this("x");
    }

    /**
     * @param variableName nombre de la variable respecto de la que se deriva.
     *                     También se utiliza para escribir los pasos
     *                     ({@code d/dx}, {@code d/dy}, {@code d/dt}…).
     */

    public DerivativeCalculator(String variableName) {
        this.variableName = Objects.requireNonNull(variableName, "variableName");
    }

    /**
     * Deriva sin registrar pasos.
     */
    public Expression derive(Expression function) {
        return derive(function, new ArrayList<>(), 0);
    }

    /**
     * Deriva y agrega a {@code steps} la explicación de cada regla aplicada.
     *
     * @param baseLevel nivel de sangría de los pasos principales
     * @return la derivada, con las simplificaciones obvias ya aplicadas
     */
    public Expression derive(Expression function, List<SolutionStep> steps, int baseLevel) {
        return new Derivation(steps).derive(function, baseLevel);
    }

    /**
     * Una derivación en curso: guarda la lista de pasos que se va llenando.
     */
    private final class Derivation {

        private final List<SolutionStep> steps;

        private Derivation(List<SolutionStep> steps) {
            this.steps = Objects.requireNonNull(steps, "steps");
        }

        private Expression derive(Expression e, int level) {
            if (!e.dependsOn(variableName)) {
                String note = e instanceof Constant ? "" : " (no depende de " + variableName + ")";
                record(level, DerivativeRule.CONSTANT, d(e) + " = 0" + note);
                return ZERO;
            }
            return switch (e) {
                case Variable v -> {
                    record(level, DerivativeRule.VARIABLE, d(v) + " = 1");
                    yield ONE;
                }
                case Sum s -> deriveTerms(s, level);
                case Difference s -> deriveTerms(s, level);
                case Negation n -> {
                    record(level, DerivativeRule.NEGATION, d(n) + " = -" + d(n.getOperand()));
                    yield negate(derive(n.getOperand(), level + 1));
                }
                case Product p -> deriveProduct(p, level);
                case Quotient q -> deriveQuotient(q, level);
                case Power p -> derivePower(p, level);
                case FunctionCall f -> deriveFunction(f, level);
                default -> throw new IllegalArgumentException("Expresión no soportada: " + e.getClass().getSimpleName());
            };
        }

        /**
         * Sumas y restas encadenadas ({@code a - b + c}) se derivan término a
         * término en un solo paso, en lugar de anidar una regla por operador.
         */
        private Expression deriveTerms(Expression e, int level) {
            List<Term> terms = new ArrayList<>();
            collectTerms(e, false, terms);
            boolean anyMinus = terms.stream().anyMatch(Term::negative);
            boolean anyPlus = terms.stream().skip(1).anyMatch(t -> !t.negative());
            DerivativeRule rule = !anyMinus ? DerivativeRule.SUM
                    : anyPlus ? DerivativeRule.SUM_AND_DIFFERENCE : DerivativeRule.DIFFERENCE;

            StringBuilder statement = new StringBuilder(d(e)).append(" = ");
            for (int i = 0; i < terms.size(); i++) {
                Term term = terms.get(i);
                if (i > 0) {
                    statement.append(term.negative() ? " - " : " + ");
                } else if (term.negative()) {
                    statement.append("-");
                }
                statement.append(d(term.expression()));
            }
            record(level, rule, statement.toString());

            Expression result = null;
            for (Term term : terms) {
                Expression derivative = derive(term.expression(), level + 1);
                if (result == null) {
                    result = term.negative() ? negate(derivative) : derivative;
                } else {
                    result = term.negative() ? difference(result, derivative) : sum(result, derivative);
                }
            }
            return result;
        }

        private Expression deriveProduct(Product p, int level) {
            Expression f = p.getLeft();
            Expression g = p.getRight();
            if (!f.dependsOn(variableName)) {
                record(level, DerivativeRule.CONSTANT_MULTIPLE, d(p) + " = " + factor(f) + "*" + d(g));
                return product(f, derive(g, level + 1));
            }
            if (!g.dependsOn(variableName)) {
                record(level, DerivativeRule.CONSTANT_MULTIPLE, d(p) + " = " + factor(g) + "*" + d(f));
                return product(g, derive(f, level + 1));
            }
            record(level, DerivativeRule.PRODUCT,
                    d(p) + " = " + d(f) + "*" + factor(g) + " + " + factor(f) + "*" + d(g));
            Expression df = derive(f, level + 1);
            Expression dg = derive(g, level + 1);
            return sum(product(df, g), product(f, dg));
        }

        private Expression deriveQuotient(Quotient q, int level) {
            Expression f = q.getLeft();
            Expression g = q.getRight();
            if (!g.dependsOn(variableName)) {
                record(level, DerivativeRule.CONSTANT_MULTIPLE, d(q) + " = " + d(f) + "/" + factor(g));
                return quotient(derive(f, level + 1), g);
            }
            record(level, DerivativeRule.QUOTIENT, d(q) + " = (" + d(f) + "*" + factor(g) + " - "
                    + factor(f) + "*" + d(g) + ")/" + base(g) + "^2");
            Expression df = derive(f, level + 1);
            Expression dg = derive(g, level + 1);
            return quotient(difference(product(df, g), product(f, dg)), power(g, TWO));
        }

        private Expression derivePower(Power p, int level) {
            Expression base = p.getLeft();
            Expression exponent = p.getRight();

            // x^n: la base depende de x y el exponente es constante
            if (!exponent.dependsOn(variableName)) {
                Expression reduced = difference(exponent, ONE);
                Expression outer = product(exponent, power(base, reduced));
                if (base instanceof Variable) {
                    record(level, DerivativeRule.POWER, d(p) + " = " + outer);
                    return outer;
                }
                record(level, DerivativeRule.POWER, d(p) + " = " + factor(outer) + "*" + d(base)
                        + "  (con regla de la cadena)");
                return chain(derive(base, level + 1), outer);
            }

            // a^u: la base es constante y el exponente depende de x
            if (!base.dependsOn(variableName)) {
                Expression outer = base == Constant.E ? p : product(p, call(MathFunction.LN, base));
                if (exponent instanceof Variable) {
                    record(level, DerivativeRule.EXPONENTIAL, d(p) + " = " + outer);
                    return outer;
                }
                record(level, DerivativeRule.EXPONENTIAL, d(p) + " = " + factor(outer) + "*" + d(exponent)
                        + "  (con regla de la cadena)");
                return chain(derive(exponent, level + 1), outer);
            }

            // f^g: base y exponente dependen de x
            record(level, DerivativeRule.LOGARITHMIC, d(p) + " = " + factor(p) + "*(" + d(exponent)
                    + "*ln(" + base + ") + " + factor(exponent) + "*" + d(base) + "/" + factor(base) + ")");
            Expression dBase = derive(base, level + 1);
            Expression dExponent = derive(exponent, level + 1);
            return product(p, sum(
                    product(dExponent, call(MathFunction.LN, base)),
                    quotient(product(exponent, dBase), base)));
        }

        private Expression deriveFunction(FunctionCall call, int level) {
            MathFunction function = call.getFunction();
            Expression u = call.getArgument();
            Expression outer = outerDerivative(function, u);
            if (u instanceof Variable) {
                record(level, functionRuleName(function), d(call) + " = " + outer);
                return outer;
            }
            record(level, DerivativeRule.CHAIN.getDisplayName() + " con la " + lowercaseFirst(functionRuleName(function)),
                    d(call) + " = " + d(u) + "*" + factor(outer));
            return chain(derive(u, level + 1), outer);
        }

        private void record(int level, DerivativeRule rule, String statement) {
            record(level, rule.getDisplayName(), statement);
        }

        private void record(int level, String title, String statement) {
            steps.add(new SolutionStep(title + ": " + statement, level));
        }
    }

    /** Un término de una suma o resta encadenada, con su signo. */
    private record Term(Expression expression, boolean negative) {
    }

    /**
     * Separa {@code a - b + c} en los términos {@code +a}, {@code -b}, {@code +c}.
     */
    private static void collectTerms(Expression e, boolean negative, List<Term> terms) {
        if (e instanceof Sum s) {
            collectTerms(s.getLeft(), negative, terms);
            collectTerms(s.getRight(), negative, terms);
        } else if (e instanceof Difference d) {
            collectTerms(d.getLeft(), negative, terms);
            collectTerms(d.getRight(), !negative, terms);
        } else {
            terms.add(new Term(e, negative));
        }
    }

    // ------------------------------------------------------------ tabla

    /**
     * Derivada de la función exterior evaluada en {@code u}: la parte
     * {@code f'(u)} de la regla de la cadena.
     */
    private static Expression outerDerivative(MathFunction function, Expression u) {
        return switch (function) {
            case SIN -> call(MathFunction.COS, u);
            case COS -> negate(call(MathFunction.SIN, u));
            case TAN -> power(call(MathFunction.SEC, u), TWO);
            case COT -> negate(power(call(MathFunction.CSC, u), TWO));
            case SEC -> product(call(MathFunction.SEC, u), call(MathFunction.TAN, u));
            case CSC -> negate(product(call(MathFunction.CSC, u), call(MathFunction.COT, u)));
            case ARCSIN -> quotient(ONE, call(MathFunction.SQRT, difference(ONE, power(u, TWO))));
            case ARCCOS -> negate(quotient(ONE, call(MathFunction.SQRT, difference(ONE, power(u, TWO)))));
            case ARCTAN -> quotient(ONE, sum(ONE, power(u, TWO)));
            case SINH -> call(MathFunction.COSH, u);
            case COSH -> call(MathFunction.SINH, u);
            case TANH -> quotient(ONE, power(call(MathFunction.COSH, u), TWO));
            case EXP -> call(MathFunction.EXP, u);
            case LN -> quotient(ONE, u);
            case LOG -> quotient(ONE, product(u, call(MathFunction.LN, new Constant(10))));
            case SQRT -> quotient(ONE, product(TWO, call(MathFunction.SQRT, u)));
            case ABS -> quotient(u, call(MathFunction.ABS, u));
        };
    }

    private static String functionRuleName(MathFunction function) {
        return "Derivada " + switch (function) {
            case SIN -> "del seno";
            case COS -> "del coseno";
            case TAN -> "de la tangente";
            case COT -> "de la cotangente";
            case SEC -> "de la secante";
            case CSC -> "de la cosecante";
            case ARCSIN -> "del arcoseno";
            case ARCCOS -> "del arcocoseno";
            case ARCTAN -> "de la arcotangente";
            case SINH -> "del seno hiperbólico";
            case COSH -> "del coseno hiperbólico";
            case TANH -> "de la tangente hiperbólica";
            case EXP -> "de la exponencial";
            case LN -> "del logaritmo natural";
            case LOG -> "del logaritmo base 10";
            case SQRT -> "de la raíz cuadrada";
            case ABS -> "del valor absoluto";
        };
    }

    // ------------------------------------------------------------ armado

    /**
     * Regla de la cadena: {@code u'·f'(u)}. Se escribe primero {@code u'}
     * ({@code 2*x*cos(x^2)}) y, si {@code f'(u)} es una fracción, {@code u'}
     * pasa al numerador ({@code 2*x/(x^2 + 1)} en vez de {@code 2*x*(1/(x^2 + 1))}).
     */
    private static Expression chain(Expression du, Expression outer) {
        if (outer instanceof Negation negation) {
            return negate(chain(du, negation.getOperand()));
        }
        if (outer instanceof Quotient q && q.getLeft() instanceof Constant c && c.isNumber()) {
            return quotient(product(c, du), q.getRight());
        }
        return product(du, outer);
    }

    private static FunctionCall call(MathFunction function, Expression argument) {
        return new FunctionCall(function, argument);
    }

    // ------------------------------------------------------------ texto

    private String d(Expression e) {
        return "d/d" + variableName + "[" + e + "]";
    }

    /** Operando de un producto en el texto: con paréntesis si es suma o resta. */
    private static String factor(Expression e) {
        return e.precedence() < Expression.MULTIPLICATIVE ? "(" + e + ")" : e.toString();
    }

    /** Base de una potencia en el texto: con paréntesis salvo números y variables. */
    private static String base(Expression e) {
        return e.precedence() < Expression.ATOM ? "(" + e + ")" : e.toString();
    }

    private static String lowercaseFirst(String text) {
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }
}
