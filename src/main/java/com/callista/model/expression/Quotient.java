package com.callista.model.expression;

/**
 * Cociente de dos expresiones: {@code a/b}. Si el divisor vale 0 el resultado es {@code Infinity} o {@code NaN}.
 */
public final class Quotient extends BinaryOperation {

    public Quotient(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public double evaluate(double x) {
        return getLeft().evaluate(x) / getRight().evaluate(x);
    }

    @Override
    public BinaryOperation withOperands(Expression left, Expression right) {
        return new Quotient(left, right);
    }

    @Override
    public int precedence() {
        return MULTIPLICATIVE;
    }

    @Override
    protected String symbol() {
        return "/";
    }
}
