package com.callista.model.expression;

/**
 * Potencia: {@code a^b}, donde {@code a} es la base y {@code b} el exponente.
 */
public final class Power extends BinaryOperation {

    public Power(Expression left, Expression right) {
        super(left, right);
    }

    /** Denominador impar máximo que se reconoce en un exponente: x^(1/3), x^(2/5). */
    private static final int MAX_ODD_DENOMINATOR = 15;

    @Override
    public double evaluate(double x) {
        return realPower(getLeft().evaluate(x), getRight().evaluate(x));
    }

    /**
     * Potencia real. Con base negativa y exponente p/q con q impar, la raíz
     * q-ésima existe: {@code (-8)^(1/3) = -2} y {@code (-8)^(2/3) = 4}
     * ({@link Math#pow} daría NaN).
     */
    static double realPower(double base, double exponent) {
        if (base >= 0 || exponent == Math.rint(exponent) || !Double.isFinite(exponent)) {
            return Math.pow(base, exponent);
        }
        for (int q = 3; q <= MAX_ODD_DENOMINATOR; q += 2) {
            double p = exponent * q;
            if (Math.abs(p - Math.rint(p)) < 1e-12) {
                double magnitude = Math.pow(-base, exponent);
                return ((long) Math.rint(p)) % 2 == 0 ? magnitude : -magnitude;
            }
        }
        return Double.NaN;
    }

    @Override
    public BinaryOperation withOperands(Expression left, Expression right) {
        return new Power(left, right);
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
