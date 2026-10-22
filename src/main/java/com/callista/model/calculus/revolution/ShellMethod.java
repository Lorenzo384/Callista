package com.callista.model.calculus.revolution;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Product;
import com.callista.model.expression.Simplifier;
import com.callista.model.expression.Variable;

/**
 * Capas cilíndricas: con un eje vertical, cada rebanada vertical de la región
 * genera un cilindro hueco de radio |x - k| y altura f(x) - g(x).
 * {@code V = 2π∫[a, b] radio(x)·altura(x) dx}.
 */
public class ShellMethod implements VolumeMethod {

    private static final Variable X = new Variable("x");

    @Override
    public String getName() {
        return "Capas cilíndricas";
    }

    @Override
    public String getFormula() {
        return "V = 2π∫[a, b] radio(x)·altura(x) dx";
    }

    @Override
    public Expression getFactor() {
        return Simplifier.simplify(new Product(new Constant(2), Constant.PI));
    }

    @Override
    public Slice slice(Curve upper, Curve lower, RotationAxis axis, double middle) {
        Radius radius = Radius.of(new Curve("x", X), axis, middle);
        Expression height = Simplifier.simplify(new Difference(upper.expression(), lower.expression()));
        String heightText = upper.name() + " - " + lower.name() + " = " + height;
        Expression integrand = Simplifier.expand(new Product(radius.value(), height));
        return new Slice(integrand, "radio = " + radius.text() + " (distancia de x al eje), altura = " + heightText);
    }
}
