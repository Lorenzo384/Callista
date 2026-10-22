package com.callista.model.expression;

import java.util.Objects;

/**
 * La variable independiente de la función, normalmente {@code x}.
 */
public final class Variable implements Expression {

    private final String name;

    public Variable(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la variable no puede estar vacío");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public double evaluate(double x) {
        return x;
    }

    @Override
    public boolean dependsOnVariable() {
        return true;
    }

    @Override
    public Expression replace(Expression target, Expression replacement) {
        return equals(target) ? replacement : this;
    }

    @Override
    public boolean contains(Expression part) {
        return equals(part);
    }

    @Override
    public int precedence() {
        return ATOM;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Variable v && name.equals(v.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
