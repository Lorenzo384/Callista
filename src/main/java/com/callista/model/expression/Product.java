package com.callista.model.expression;

/**
 * Producto de dos expresiones: {@code a*b}.
 */
public final class Product extends BinaryOperation {

    public Product(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public double evaluate(double x) {
        return getLeft().evaluate(x) * getRight().evaluate(x);
    }

    @Override
    public int precedence() {
        return MULTIPLICATIVE;
    }

    @Override
    protected boolean isAssociative() {
        return true;
    }

    @Override
    protected String symbol() {
        return "*";
    }
}
