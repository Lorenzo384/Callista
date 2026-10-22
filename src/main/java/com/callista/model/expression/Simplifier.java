package com.callista.model.expression;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Simplificación de expresiones, en dos niveles:
 *
 * <ol>
 *     <li><b>{@link #simplify(Expression)}</b>: simplificación completa. Agrupa
 *     términos semejantes ({@code 2x + 3x = 5x}), aplica las leyes de los
 *     exponentes ({@code x·x^2 = x^3}, {@code (x^2)^2 = x^4}), cancela factores
 *     ({@code x/x = 1}), opera números con fracciones exactas, sustituye valores
 *     conocidos ({@code sen(0) = 0}) y ordena los términos de mayor a menor
 *     grado. Ver {@link Normalizer}.</li>
 *     <li><b>Constructores</b> ({@link #sum}, {@link #product}…): simplifican solo
 *     lo obvio al armar una expresión nueva (en lugar de {@code new Sum(a, b)} se
 *     usa {@code Simplifier.sum(a, b)}, que devuelve {@code a} si {@code b} es 0).
 *     Los usa el motor de derivadas para que el resultado sin simplificar ya sea
 *     legible.</li>
 * </ol>
 *
 * <p>Reglas de los constructores:</p>
 * <ul>
 *     <li>Neutros: {@code a + 0 = a}, {@code a·1 = a}, {@code a/1 = a}, {@code a^1 = a}.</li>
 *     <li>Absorbentes: {@code a·0 = 0}, {@code 0/a = 0}, {@code a^0 = 1}, {@code 1^a = 1}.</li>
 *     <li>Operaciones entre números: {@code 2 + 3 = 5}; los coeficientes se juntan al
 *     frente: {@code (2·x)·(3·y) = 6·x·y}.</li>
 *     <li>Signos: {@code a + (-b) = a - b}, {@code a + (-1/x) = a - 1/x}, {@code -(-a) = a},
 *     {@code (-1)·a = -a}.</li>
 *     <li>El número va primero en un producto: {@code x·3 = 3·x}.</li>
 * </ul>
 *
 * <p>Las constantes con nombre ({@code π}, {@code e}) no se operan, para no
 * perder cómo se escribieron.</p>
 */
public final class Simplifier {

    private static final Constant ZERO = new Constant(0);
    private static final Constant ONE = new Constant(1);

    private Simplifier() {
    }

    /**
     * Simplificación completa.
     */
    public static Expression simplify(Expression expression) {
        return simplify(expression, EnumSet.noneOf(SimplificationRule.class));
    }

    /**
     * Los términos de la expresión ya simplificada: {@code 3*x^2 - sin(x)} da
     * dos {@link Monomial}.
     */
    public static List<Monomial> terms(Expression expression) {
        return new Normalizer(EnumSet.noneOf(SimplificationRule.class)).monomials(expression);
    }

    /**
     * Valor exacto de una expresión numérica sencilla ({@code 3}, {@code -1},
     * {@code 2/3}), o vacío si no lo es (por ejemplo {@code x} o {@code π}).
     */
    public static Optional<Fraction> numericValue(Expression expression) {
        try {
            return Normalizer.numeric(expression);
        } catch (ArithmeticException tooLarge) {
            return Optional.empty();
        }
    }

    /**
     * Simplifica y además desarrolla los productos y potencias enteras de
     * sumas: {@code x*(x + 1) = x^2 + x}.
     */
    public static Expression expand(Expression expression) {
        try {
            return new Normalizer(EnumSet.noneOf(SimplificationRule.class)).expand(expression);
        } catch (ArithmeticException tooLarge) {
            return simplify(expression);
        }
    }

    /**
     * Simplificación completa que además informa qué tipos de simplificación
     * se aplicaron, para explicarlos en el procedimiento.
     *
     * @param applied conjunto donde se agregan las reglas aplicadas
     * @return la expresión simplificada; si algún número es demasiado grande
     *         para operarlo de forma exacta, la expresión original
     */
    public static Expression simplify(Expression expression, Set<SimplificationRule> applied) {
        try {
            return new Normalizer(applied).simplify(expression);
        } catch (ArithmeticException tooLarge) {
            return expression;
        }
    }

    public static Expression sum(Expression a, Expression b) {
        a = normalize(a);
        b = normalize(b);
        if (isNumber(a, 0)) {
            return b;
        }
        if (isNumber(b, 0)) {
            return a;
        }
        if (isNumber(a) && isNumber(b)) {
            return number(value(a) + value(b));
        }
        if (startsNegative(b)) {
            return difference(a, negate(b));
        }
        return new Sum(a, b);
    }

    public static Expression difference(Expression a, Expression b) {
        a = normalize(a);
        b = normalize(b);
        if (isNumber(b, 0)) {
            return a;
        }
        if (isNumber(a, 0)) {
            return negate(b);
        }
        if (isNumber(a) && isNumber(b)) {
            return number(value(a) - value(b));
        }
        if (startsNegative(b)) {
            return sum(a, negate(b));
        }
        return new Difference(a, b);
    }

    public static Expression product(Expression a, Expression b) {
        a = normalize(a);
        b = normalize(b);
        if (isNumber(a, 0) || isNumber(b, 0)) {
            return ZERO;
        }
        if (isNumber(a, 1)) {
            return b;
        }
        if (isNumber(b, 1)) {
            return a;
        }
        if (isNumber(a) && isNumber(b)) {
            return number(value(a) * value(b));
        }
        if (isNumber(a, -1)) {
            return negate(b);
        }
        if (isNumber(b, -1)) {
            return negate(a);
        }
        if (a instanceof Negation negation) {
            return negate(product(negation.getOperand(), b));
        }
        if (b instanceof Negation negation) {
            return negate(product(a, negation.getOperand()));
        }
        if (isNumber(b) && !isNumber(a)) {
            return product(b, a);
        }
        if (hasCoefficient(a) || hasCoefficient(b)) {
            double coefficient = coefficient(a) * coefficient(b);
            Expression rest = product(withoutCoefficient(a), withoutCoefficient(b));
            if (isNumber(rest)) {
                return number(coefficient * value(rest));
            }
            if (coefficient == 1) {
                return rest;
            }
            if (coefficient == -1) {
                return negate(rest);
            }
            return new Product(number(coefficient), rest);
        }
        return new Product(a, b);
    }

    public static Expression quotient(Expression a, Expression b) {
        a = normalize(a);
        b = normalize(b);
        if (isNumber(b, 1)) {
            return a;
        }
        if (isNumber(a, 0) && !isNumber(b, 0)) {
            return ZERO;
        }
        if (isNumber(a) && isNumber(b) && value(b) != 0 && value(a) % value(b) == 0) {
            return number(value(a) / value(b));
        }
        if (a instanceof Negation negation) {
            return negate(quotient(negation.getOperand(), b));
        }
        return new Quotient(a, b);
    }

    public static Expression power(Expression base, Expression exponent) {
        base = normalize(base);
        exponent = normalize(exponent);
        if (isNumber(exponent, 0)) {
            return ONE;
        }
        if (isNumber(exponent, 1)) {
            return base;
        }
        if (isNumber(base, 1)) {
            return ONE;
        }
        if (isNumber(base) && isNumber(exponent)) {
            double result = Math.pow(value(base), value(exponent));
            if (result == Math.rint(result) && Double.isFinite(result)) {
                return number(result);
            }
        }
        return new Power(base, exponent);
    }

    public static Expression negate(Expression a) {
        a = normalize(a);
        if (isNumber(a)) {
            return number(-value(a));
        }
        if (a instanceof Negation negation) {
            return negation.getOperand();
        }
        if (a instanceof Product p && isNumber(p.getLeft())) {
            return product(number(-value(p.getLeft())), p.getRight());
        }
        if (a instanceof Quotient q && startsNegative(q.getLeft())) {
            return quotient(negate(q.getLeft()), q.getRight());
        }
        return new Negation(a);
    }

    /**
     * El parser escribe {@code -2} como signo negativo aplicado a 2; aquí se
     * trata como el número −2 para poder operarlo.
     */
    private static Expression normalize(Expression e) {
        if (e instanceof Negation negation && isNumber(negation.getOperand())) {
            return number(-value(negation.getOperand()));
        }
        return e;
    }

    /**
     * Indica si la expresión se escribe empezando con signo negativo:
     * {@code -2}, {@code -x}, {@code -3*x} o {@code -1/x^2}. Sirve para escribir
     * {@code a - 1/x^2} en lugar de {@code a + -1/x^2}.
     */
    private static boolean startsNegative(Expression e) {
        return (isNumber(e) && value(e) < 0)
                || e instanceof Negation
                || (e instanceof Product p && startsNegative(p.getLeft()))
                || (e instanceof Quotient q && startsNegative(q.getLeft()));
    }

    /** Producto que empieza con un número distinto de 1, como {@code 3*x}. */
    private static boolean hasCoefficient(Expression e) {
        return e instanceof Product p && isNumber(p.getLeft());
    }

    private static double coefficient(Expression e) {
        if (isNumber(e)) {
            return value(e);
        }
        return hasCoefficient(e) ? value(((Product) e).getLeft()) : 1;
    }

    private static Expression withoutCoefficient(Expression e) {
        if (isNumber(e)) {
            return ONE;
        }
        return hasCoefficient(e) ? ((Product) e).getRight() : e;
    }

    private static boolean isNumber(Expression expression) {
        return expression instanceof Constant constant && constant.isNumber();
    }

    private static boolean isNumber(Expression expression, double expected) {
        return expression instanceof Constant constant && constant.isNumber(expected);
    }

    private static double value(Expression number) {
        return ((Constant) number).getValue();
    }

    private static Constant number(double value) {
        // Evita mostrar «-0»
        return new Constant(value == 0 ? 0 : value);
    }
}
