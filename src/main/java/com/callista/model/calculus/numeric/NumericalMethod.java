package com.callista.model.calculus.numeric;

/**
 * Los métodos numéricos disponibles, para elegirlos por nombre.
 */
public enum NumericalMethod {

    SIMPSON(new SimpsonRule()),
    TRAPEZOIDAL(new TrapezoidalRule()),
    MIDPOINT(new MidpointRule());

    private final WeightedRule rule;

    NumericalMethod(WeightedRule rule) {
        this.rule = rule;
    }

    public WeightedRule getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return rule.getShortName();
    }
}
