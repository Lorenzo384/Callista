package com.callista.model.expression;

/**
 * Potencia: {@code a^b}, donde {@code a} es la base y {@code b} el exponente.
 */
public final class Power extends BinaryOperation {

    public Power(Expression left, Expression right) {
        super(left, right);
    }

    @Override
    public double evaluate(double x) {
        return Math.pow(getLeft().evaluate(x), getRight().evaluate(x));
    }

    @Override
    public int precedence() {
        return POWER;
    }

    @Override
    protected String symbol() {
        return "^";
    }

    /** {@code 2^3^2} se lee como {@code 2^(3^2)}, igual que en matemáticas. */
    @Override
    protected boolean isRightAssociative() {
        return true;
    }
}
