package com.callista.model.expression;

/**
 * Suma de dos expresiones: {@code a + b}.
 */
public final class Sum extends BinaryOperation {

    public Sum(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public double evaluate(double x) {
        return getLeft().evaluate(x) + getRight().evaluate(x);
    }

    @Override
    public int precedence() {
        return ADDITIVE;
    }

    @Override
    protected String symbol() {
        return "+";
    }
}
