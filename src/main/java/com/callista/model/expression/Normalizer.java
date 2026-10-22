package com.callista.model.expression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Lleva una expresión a su <b>forma normal</b>: una suma de términos, donde
 * cada término es un coeficiente racional por un producto de factores con
 * exponente. Por ejemplo, {@code 2*x/(x^2)^2 + 3} queda como dos términos:
 *
 * <pre>
 *   2 · {x: 1, x: -4}  →  2 · {x: -3}     (leyes de los exponentes)
 *   3 · {}
 * </pre>
 *
 * <p>En esa forma es fácil agrupar términos semejantes (mismos factores),
 * sumar exponentes de factores con la misma base y cancelar los que quedan
 * con exponente 0. Después se reconstruye la expresión en el orden habitual:
 * términos de mayor a menor grado, número al frente y exponentes negativos
 * en el denominador.</p>
 *
 * <p>Uso interno de {@link Simplifier#simplify}.</p>
 */
final class Normalizer {

    /** Potencia máxima de una suma que se desarrolla: (x + 1)^8. */
    private static final int MAX_EXPANSION_POWER = 8;

    /** Denominador máximo de un coeficiente que se escribe como fracción. */
    private static final long MAX_FRACTION_DENOMINATOR = 1000;

    /** Diferencia máxima para reconocer un valor exacto: sen(π) da 1.2e-16 y no 0. */
    private static final double KNOWN_VALUE_TOLERANCE = 1e-12;

    private final Set<SimplificationRule> applied;

    Normalizer(Set<SimplificationRule> applied) {
        this.applied = applied;
    }

    Expression simplify(Expression e) {
        return switch (e) {
            case Constant c -> c;
            case Variable v -> v;
            case FunctionCall f when f.getFunction() != MathFunction.SQRT -> simplifyFunction(f);
            default -> build(terms(e));
        };
    }

    /**
     * Los términos de la expresión simplificada, para quien necesite analizar
     * su forma (por ejemplo, el integrador).
     */
    List<Monomial> monomials(Expression e) {
        return terms(e).stream().map(t -> new Monomial(t.coefficient(), t.factors())).toList();
    }

    /**
     * Desarrolla los productos y potencias enteras de sumas:
     * {@code x*(x + 1) = x^2 + x}, {@code (x + 1)^2 = x^2 + 2*x + 1}.
     * Las sumas en el denominador no se tocan.
     */
    Expression expand(Expression e) {
        List<Term> expanded = new ArrayList<>();
        for (Term term : terms(e)) {
            expanded.addAll(expandTerm(term));
        }
        return build(combine(expanded));
    }

    private List<Term> expandTerm(Term term) {
        List<Term> partial = List.of(Term.number(term.coefficient()));
        for (Map.Entry<Expression, Expression> factor : term.factors().entrySet()) {
            Expression base = factor.getKey();
            Optional<Fraction> n = numeric(factor.getValue());
            boolean expandable = (base instanceof Sum || base instanceof Difference)
                    && n.isPresent() && n.get().isInteger() && n.get().signum() > 0
                    && n.get().getNumerator() <= MAX_EXPANSION_POWER;
            if (expandable) {
                List<Term> inner = terms(base);
                for (long k = 0; k < n.get().getNumerator(); k++) {
                    partial = multiplyAll(partial, inner);
                }
            } else {
                partial = multiplyAll(partial, List.of(Term.factor(base, factor.getValue())));
            }
        }
        return partial;
    }

    private List<Term> multiplyAll(List<Term> left, List<Term> right) {
        List<Term> products = new ArrayList<>();
        for (Term a : left) {
            for (Term b : right) {
                products.add(multiply(a, b));
            }
        }
        return combine(products);
    }

    /** Arma la expresión de un término dado por su coeficiente y factores. */
    static Expression buildMonomial(Fraction coefficient, Map<Expression, Expression> factors) {
        return buildTerm(new Term(coefficient, new LinkedHashMap<>(factors)));
    }

    /**
     * Simplifica el argumento y, si la función queda aplicada a una constante
     * con valor entero, la sustituye: {@code sen(0) = 0}, {@code ln(e) = 1}.
     */
    private Expression simplifyFunction(FunctionCall call) {
        Expression argument = simplify(call.getArgument());
        FunctionCall simplified = new FunctionCall(call.getFunction(), argument);
        if (!argument.dependsOnVariable()) {
            double value = simplified.evaluate(0);
            double rounded = Math.rint(value);
            if (Double.isFinite(value) && Math.abs(value - rounded) < KNOWN_VALUE_TOLERANCE) {
                applied.add(SimplificationRule.KNOWN_VALUES);
                return new Constant(rounded == 0 ? 0 : rounded);
            }
        }
        return simplified;
    }

    // ------------------------------------------------------------ términos

    /** Coeficiente por factores {base → exponente}, en orden de aparición. */
    private record Term(Fraction coefficient, Map<Expression, Expression> factors) {

        static Term number(Fraction value) {
            return new Term(value, new LinkedHashMap<>());
        }

        static Term factor(Expression base, Expression exponent) {
            Map<Expression, Expression> factors = new LinkedHashMap<>();
            factors.put(base, exponent);
            return new Term(Fraction.ONE, factors);
        }

        Term negate() {
            return new Term(coefficient.negate(), factors);
        }

        Term withCoefficient(Fraction value) {
            return new Term(value, factors);
        }

        boolean dependsOnVariable() {
            return factors.keySet().stream().anyMatch(Expression::dependsOnVariable)
                    || factors.values().stream().anyMatch(Expression::dependsOnVariable);
        }
    }

    /**
     * Separa una suma o resta en términos y agrupa los semejantes.
     */
    private List<Term> terms(Expression e) {
        List<Term> collected = new ArrayList<>();
        collect(e, false, collected);
        return combine(collected);
    }

    private void collect(Expression e, boolean negative, List<Term> out) {
        switch (e) {
            case Sum s -> {
                collect(s.getLeft(), negative, out);
                collect(s.getRight(), negative, out);
            }
            case Difference d -> {
                collect(d.getLeft(), negative, out);
                collect(d.getRight(), !negative, out);
            }
            case Negation n -> collect(n.getOperand(), !negative, out);
            default -> {
                Term term = term(e);
                out.add(negative ? term.negate() : term);
            }
        }
    }

    /**
     * Agrupa los términos con los mismos factores sumando sus coeficientes
     * ({@code 2*x + 3*x = 5*x}) y quita los que valen 0.
     */
    private List<Term> combine(List<Term> terms) {
        Map<Map<Expression, Expression>, Term> grouped = new LinkedHashMap<>();
        for (Term term : terms) {
            if (term.coefficient().isZero()) {
                applied.add(SimplificationRule.NUMBERS);
                continue;
            }
            Map<Expression, Expression> key = Map.copyOf(term.factors());
            Term existing = grouped.get(key);
            if (existing == null) {
                grouped.put(key, term);
            } else {
                applied.add(key.isEmpty() ? SimplificationRule.NUMBERS : SimplificationRule.LIKE_TERMS);
                grouped.put(key, existing.withCoefficient(existing.coefficient().add(term.coefficient())));
            }
        }
        return grouped.values().stream().filter(t -> !t.coefficient().isZero()).toList();
    }

    /**
     * Convierte un término (algo que no es suma ni resta) a su forma
     * coeficiente·factores.
     */
    private Term term(Expression e) {
        return switch (e) {
            case Constant c when c.isNumber() -> Term.number(Fraction.fromDouble(c.getValue()));
            case Negation n -> term(n.getOperand()).negate();
            case Product p -> multiply(term(p.getLeft()), term(p.getRight()));
            case Quotient q -> divide(q);
            case Power p -> power(p);
            case Sum s -> sumAsFactor(s);
            case Difference d -> sumAsFactor(d);
            // √a se trata como a^(1/2): así √x·√x = x y √x·x = x^(3/2)
            case FunctionCall f when f.getFunction() == MathFunction.SQRT ->
                    power(new Power(f.getArgument(), new Quotient(new Constant(1), new Constant(2))));
            case FunctionCall f -> {
                Expression simplified = simplifyFunction(f);
                yield simplified instanceof Constant c && c.isNumber()
                        ? Term.number(Fraction.fromDouble(c.getValue()))
                        : Term.factor(simplified, one());
            }
            default -> Term.factor(e, one()); // variable, π, e
        };
    }

    /**
     * Una suma dentro de un producto, como {@code (x + 1)} en {@code 2*(x + 1)}.
     * No se desarrolla: se simplifica por dentro y queda como un factor. Si se
     * reduce a un solo término ({@code 3*(x + x)}), se usa ese término.
     */
    private Term sumAsFactor(Expression sum) {
        List<Term> inner = terms(sum);
        if (inner.isEmpty()) {
            return Term.number(Fraction.ZERO);
        }
        if (inner.size() == 1) {
            return inner.getFirst();
        }
        return Term.factor(build(inner), one());
    }

    private Term multiply(Term a, Term b) {
        if (a.coefficient().isZero() || b.coefficient().isZero()) {
            return Term.number(Fraction.ZERO);
        }
        if (!isUnit(a.coefficient()) && !isUnit(b.coefficient())) {
            applied.add(SimplificationRule.NUMBERS);
        }
        Map<Expression, Expression> factors = new LinkedHashMap<>(a.factors());
        b.factors().forEach((base, exponent) -> {
            Expression existing = factors.get(base);
            if (existing == null) {
                factors.put(base, exponent);
                return;
            }
            Expression sum = addExponents(existing, exponent);
            if (numeric(sum).map(Fraction::isZero).orElse(false)) {
                applied.add(SimplificationRule.CANCELLATION);
                factors.remove(base);
            } else {
                applied.add(SimplificationRule.EXPONENT_LAWS);
                factors.put(base, sum);
            }
        });
        return withoutEvenAbsolutes(new Term(a.coefficient().multiply(b.coefficient()), factors));
    }

    /**
     * Un valor absoluto con exponente par es la potencia del argumento:
     * {@code |x|^2 = x^2}, {@code 1/|x|^2 = 1/x^2}.
     */
    private Term withoutEvenAbsolutes(Term t) {
        for (Map.Entry<Expression, Expression> factor : t.factors().entrySet()) {
            Optional<Fraction> n = numeric(factor.getValue());
            if (factor.getKey() instanceof FunctionCall call && call.getFunction() == MathFunction.ABS
                    && n.isPresent() && n.get().isInteger() && n.get().getNumerator() % 2 == 0) {
                Map<Expression, Expression> rest = new LinkedHashMap<>(t.factors());
                rest.remove(call);
                applied.add(SimplificationRule.EXPONENT_LAWS);
                return multiply(new Term(t.coefficient(), rest), raise(term(call.getArgument()), n.get().getNumerator()));
            }
        }
        return t;
    }

    /**
     * {@code a/b} es {@code a·b^(-1)}. Si el divisor vale 0, la división se
     * deja como está.
     */
    private Term divide(Quotient q) {
        Term numerator = term(q.getLeft());
        Term denominator = term(q.getRight());
        if (denominator.coefficient().isZero()) {
            return Term.factor(new Quotient(build(List.of(numerator)), build(List.of(denominator))), one());
        }
        return multiply(numerator, reciprocal(denominator));
    }

    private Term reciprocal(Term t) {
        Map<Expression, Expression> factors = new LinkedHashMap<>();
        t.factors().forEach((base, exponent) -> factors.put(base, negateExponent(exponent)));
        return new Term(t.coefficient().reciprocal(), factors);
    }

    /**
     * Potencias. Con exponente entero se aplica a todo el término:
     * {@code (2*x^3)^2 = 4*x^6}. Con exponente fraccionario o con variable,
     * la potencia queda como un factor: {@code (x^2)^(1/2)} no se cambia por
     * {@code x}, porque en realidad vale {@code |x|}.
     */
    private Term power(Power p) {
        Expression exponent = simplify(p.getRight());
        Optional<Fraction> n = numeric(exponent);
        if (n.isPresent() && n.get().isInteger()) {
            Term base = term(p.getLeft());
            if (base.factors().isEmpty() && base.coefficient().isZero() && n.get().signum() < 0) {
                return Term.factor(new Power(build(List.of(base)), exponent), one()); // 0^-n: se deja igual
            }
            if (p.getLeft() instanceof Power || p.getLeft() instanceof Product || p.getLeft() instanceof Quotient) {
                applied.add(SimplificationRule.EXPONENT_LAWS);
            } else if (base.factors().isEmpty() && !n.get().isOne()) {
                applied.add(SimplificationRule.NUMBERS);
            }
            return raise(base, n.get().getNumerator());
        }
        Expression base = simplify(p.getLeft());
        if (n.isPresent() && base instanceof Constant c && c.isNumber()) {
            double value = Math.pow(c.getValue(), n.get().doubleValue());
            if (Double.isFinite(value) && value == Math.rint(value)) {
                applied.add(SimplificationRule.NUMBERS);
                return Term.number(Fraction.fromDouble(value)); // 4^(1/2) = 2
            }
        }
        return Term.factor(base, exponent);
    }

    private Term raise(Term t, long n) {
        Map<Expression, Expression> factors = new LinkedHashMap<>();
        if (n != 0) {
            t.factors().forEach((base, exponent) -> factors.put(base, multiplyExponent(exponent, n)));
        } else if (!t.factors().isEmpty()) {
            applied.add(SimplificationRule.EXPONENT_LAWS); // x^0 = 1
        }
        return withoutEvenAbsolutes(new Term(t.coefficient().pow(n), factors));
    }

    // ------------------------------------------------------------ exponentes

    private static Expression one() {
        return new Constant(1);
    }

    /**
     * Valor exacto de un exponente numérico: {@code 2}, {@code -1}, {@code 1/3}.
     */
    static Optional<Fraction> numeric(Expression e) {
        return switch (e) {
            case Constant c when c.isNumber() -> Optional.of(Fraction.fromDouble(c.getValue()));
            case Negation n -> numeric(n.getOperand()).map(Fraction::negate);
            case Quotient q -> numeric(q.getLeft()).flatMap(top -> numeric(q.getRight())
                    .filter(bottom -> !bottom.isZero())
                    .map(bottom -> top.multiply(bottom.reciprocal())));
            default -> Optional.empty();
        };
    }

    private Expression addExponents(Expression a, Expression b) {
        Optional<Fraction> x = numeric(a);
        Optional<Fraction> y = numeric(b);
        if (x.isPresent() && y.isPresent()) {
            return x.get().add(y.get()).toExpression();
        }
        return simplify(new Sum(a, b));
    }

    private Expression negateExponent(Expression e) {
        return numeric(e).map(f -> f.negate().toExpression()).orElseGet(() -> simplify(new Negation(e)));
    }

    private Expression multiplyExponent(Expression e, long n) {
        return numeric(e).map(f -> f.multiply(Fraction.of(n)).toExpression())
                .orElseGet(() -> simplify(new Product(new Constant(n), e)));
    }

    private static boolean isUnit(Fraction f) {
        return f.isOne() || f.negate().isOne();
    }

    // ------------------------------------------------------------ reconstrucción

    /**
     * Arma la suma: términos de mayor a menor grado en x y las constantes al
     * final ({@code 3*x^2 - 4 + 1/x}); los negativos se escriben como resta.
     * Si la suma empezaría con un término negativo, se adelanta uno positivo
     * del mismo grado: {@code 1 - ln(x)} en lugar de {@code -ln(x) + 1}.
     */
    private Expression build(List<Term> terms) {
        if (terms.isEmpty()) {
            return new Constant(0);
        }
        List<Term> ordered = new ArrayList<>(terms.stream()
                .sorted(Comparator.comparingDouble((Term t) -> -degree(t))
                        .thenComparingInt(t -> t.dependsOnVariable() ? 0 : 1))
                .toList());
        if (ordered.getFirst().coefficient().signum() < 0) {
            double leadingDegree = degree(ordered.getFirst());
            ordered.stream()
                    .filter(t -> t.coefficient().signum() > 0 && degree(t) == leadingDegree)
                    .findFirst()
                    .ifPresent(positive -> {
                        ordered.remove(positive);
                        ordered.addFirst(positive);
                    });
        }
        Expression result = null;
        for (Term term : ordered) {
            if (result == null) {
                result = buildTerm(term);
            } else if (term.coefficient().signum() < 0) {
                result = new Difference(result, buildTerm(term.negate()));
            } else {
                result = new Sum(result, buildTerm(term));
            }
        }
        return result;
    }

    /**
     * Grado del término si es polinómico ({@code 3*x^2} → 2, {@code 1/x} → -1);
     * 0 si no tiene x o si tiene otras funciones ({@code x*cos(x)}), que así
     * conservan el orden en que aparecieron.
     */
    private static double degree(Term term) {
        double degree = 0;
        for (Map.Entry<Expression, Expression> factor : term.factors().entrySet()) {
            Expression base = factor.getKey();
            Optional<Fraction> exponent = numeric(factor.getValue());
            if (base instanceof Variable && exponent.isPresent()) {
                degree = exponent.get().doubleValue();
            } else if (base.dependsOnVariable() || factor.getValue().dependsOnVariable()) {
                return 0;
            }
        }
        return degree;
    }

    /**
     * Arma un término: número al frente, factores en orden (constantes,
     * potencias de x, funciones, sumas) y exponentes negativos en el
     * denominador: {@code 2·{x:1, (x^2+1):-1}} se escribe {@code 2*x/(x^2 + 1)}.
     */
    private static Expression buildTerm(Term term) {
        Fraction c = term.coefficient();
        List<Expression> numerator = new ArrayList<>();
        List<Expression> denominator = new ArrayList<>();
        term.factors().entrySet().stream()
                .sorted(Comparator.comparingInt(factor -> group(factor.getKey(), factor.getValue())))
                .forEach(factor -> {
                    Expression base = factor.getKey();
                    Expression exponent = factor.getValue();
                    Optional<Fraction> n = numeric(exponent);
                    if (n.isPresent() && n.get().signum() < 0) {
                        denominator.add(power(base, n.get().negate()));
                    } else {
                        numerator.add(n.map(f -> power(base, f)).orElseGet(() -> new Power(base, exponent)));
                    }
                });

        // Fracciones «de clase» (1/3, 5/6) se escriben como fracción; las que
        // vienen de decimales largos (5403/10000) se escriben como decimal.
        boolean asDecimal = c.getDenominator() > MAX_FRACTION_DENOMINATOR;
        if (asDecimal) {
            numerator.addFirst(new Constant(Math.abs(c.doubleValue())));
        } else {
            long top = Math.absExact(c.getNumerator());
            if (top != 1 || numerator.isEmpty()) {
                numerator.addFirst(new Constant(top));
            }
        }
        if (c.signum() < 0) {
            Expression first = numerator.getFirst();
            numerator.set(0, first instanceof Constant k ? new Constant(-k.getValue()) : new Negation(first));
        }
        if (!asDecimal && c.getDenominator() != 1) {
            denominator.addFirst(new Constant(c.getDenominator()));
        }
        Expression result = productOf(numerator);
        return denominator.isEmpty() ? result : new Quotient(result, productOf(denominator));
    }

    /**
     * Orden de los factores dentro de un término: 0 constantes ({@code π}),
     * 1 potencias de x, 2 funciones y exponenciales ({@code sin(x)}, {@code e^x}),
     * 3 sumas.
     */
    private static int group(Expression base, Expression exponent) {
        boolean numericExponent = numeric(exponent).isPresent();
        if (!base.dependsOnVariable() && numericExponent) {
            return 0;
        }
        if (base instanceof Variable && numericExponent) {
            return 1;
        }
        if (base instanceof FunctionCall || !numericExponent) {
            return 2;
        }
        return 3;
    }

    /** {@code base^exponent}; el exponente 1/2 se escribe como raíz: {@code sqrt(base)}. */
    private static Expression power(Expression base, Fraction exponent) {
        if (exponent.isOne()) {
            return base;
        }
        if (exponent.equals(Fraction.of(1, 2))) {
            return new FunctionCall(MathFunction.SQRT, base);
        }
        return new Power(base, exponent.toExpression());
    }

    private static Expression productOf(List<Expression> factors) {
        Expression result = factors.getFirst();
        for (int i = 1; i < factors.size(); i++) {
            result = new Product(result, factors.get(i));
        }
        return result;
    }
}
