package com.callista.model.expression;

import java.util.Objects;

/**
 * Signo negativo aplicado a una expresión: {@code -a}.
 */
public final class Negation implements Expression {

    private final Expression operand;

    public Negation(Expression operand) {
        this.operand = Objects.requireNonNull(operand, "operand");
    }

    public Expression getOperand() {
        return operand;
    }

    @Override
    public double evaluate(double x) {
        return -operand.evaluate(x);
    }

    @Override
    public boolean dependsOnVariable() {
        return operand.dependsOnVariable();
    }

    @Override
    public Expression replace(Expression target, Expression replacement) {
        return equals(target) ? replacement : new Negation(operand.replace(target, replacement));
    }

    @Override
    public boolean contains(Expression part) {
        return equals(part) || operand.contains(part);
    }

    @Override
    public int precedence() {
        return UNARY;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Negation n && operand.equals(n.operand);
    }

    @Override
    public int hashCode() {
        return -operand.hashCode();
    }

    /**
     * La potencia agrupa más fuerte que el signo: {@code -x^2} es {@code -(x^2)},
     * mientras que {@code -(x + 1)} y {@code -(-x)} necesitan paréntesis.
     */
    @Override
    public String toString() {
        boolean parentheses = operand.precedence() <= UNARY;
        return "-" + (parentheses ? "(" + operand + ")" : operand.toString());
    }
}
