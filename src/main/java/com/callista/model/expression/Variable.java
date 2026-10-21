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
    public int precedence() {
        return ATOM;
    }

    @Override
    public String toString() {
        return name;
    }
}
