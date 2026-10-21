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
    public int precedence() {
        return UNARY;
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
