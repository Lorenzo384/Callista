package com.callista.model.expression;

import java.util.Objects;

/**
 * Base común de las operaciones con dos operandos: suma, resta, producto,
 * cociente y potencia.
 *
 * <p>Guarda los dos hijos y sabe convertirse a texto colocando paréntesis
 * solo donde la precedencia lo exige. Cada subclase define su símbolo,
 * su precedencia y cómo se evalúa.</p>
 */
public abstract class BinaryOperation implements Expression {

    private final Expression left;
    private final Expression right;

    protected BinaryOperation(Expression left, Expression right) {
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public boolean dependsOnVariable() {
        return left.dependsOnVariable() || right.dependsOnVariable();
    }

    /**
     * Símbolo del operador, por ejemplo {@code "+"} o {@code "^"}.
     */
    protected abstract String symbol();

    /**
     * Indica si la operación agrupa de derecha a izquierda.
     * Solo la potencia lo hace: {@code 2^3^2} significa {@code 2^(3^2)}.
     */
    protected boolean isRightAssociative() {
        return false;
    }

    /**
     * Indica si la operación es asociativa, es decir, si {@code a ∘ (b ∘ c)}
     * vale lo mismo que {@code (a ∘ b) ∘ c}. La suma y el producto lo son, así
     * que {@code x*(2*y)} se escribe {@code x*2*y}; la resta y la división no:
     * {@code x - (y + 1)} conserva sus paréntesis.
     */
    protected boolean isAssociative() {
        return false;
    }

    @Override
    public String toString() {
        String separator = precedence() == ADDITIVE ? " " + symbol() + " " : symbol();
        boolean rightTie = !isRightAssociative() && !isAssociative();
        return wrap(left, needsParentheses(left, isRightAssociative()))
                + separator
                + wrap(right, needsParentheses(right, rightTie) || isNegative(right));
    }

    /**
     * Un hijo necesita paréntesis si su operador agrupa más débil que este,
     * o si agrupa igual pero está en el lado contrario a la asociatividad,
     * por ejemplo {@code x - (y + z)} o {@code (x^2)^3}.
     */
    private boolean needsParentheses(Expression child, boolean parenthesizeOnTie) {
        int childPrecedence = child.precedence();
        return childPrecedence < precedence()
                || (childPrecedence == precedence() && parenthesizeOnTie);
    }

    /**
     * Un operando negativo a la derecha se encierra para leerse mejor:
     * {@code x*(-y)} en lugar de {@code x*-y}.
     */
    private static boolean isNegative(Expression expression) {
        return expression.precedence() == UNARY;
    }

    private static String wrap(Expression expression, boolean parentheses) {
        return parentheses ? "(" + expression + ")" : expression.toString();
    }
}
