package com.callista.model.expression;

import java.util.Objects;

/**
 * Aplicación de una función elemental a una expresión: {@code sin(x^2)},
 * {@code ln(x + 1)}.
 */
public final class FunctionCall implements Expression {

    private final MathFunction function;
    private final Expression argument;

    public FunctionCall(MathFunction function, Expression argument) {
        this.function = Objects.requireNonNull(function, "function");
        this.argument = Objects.requireNonNull(argument, "argument");
    }

    public MathFunction getFunction() {
        return function;
    }

    public Expression getArgument() {
        return argument;
    }

    @Override
    public double evaluate(double x) {
        return function.apply(argument.evaluate(x));
    }

    @Override
    public boolean dependsOnVariable() {
        return argument.dependsOnVariable();
    }

    /**
     * El argumento ya va entre paréntesis, así que la llamada nunca necesita
     * paréntesis extra: {@code sin(x)^2}, {@code -ln(x)}.
     */
    @Override
    public int precedence() {
        return ATOM;
    }

    @Override
    public String toString() {
        return function.getName() + "(" + argument + ")";
    }
}
