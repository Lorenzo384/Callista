package com.callista.model.expression;

/**
 * Resta de dos expresiones: {@code a - b}.
 */
public final class Difference extends BinaryOperation {

    public Difference(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public double evaluate(double x) {
        return getLeft().evaluate(x) - getRight().evaluate(x);
    }

    @Override
    public int precedence() {
        return ADDITIVE;
    }

    @Override
    protected String symbol() {
        return "-";
    }
}
