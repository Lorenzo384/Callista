package com.callista.model.calculus.revolution;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Power;
import com.callista.model.expression.Simplifier;

/**
 * Discos: la región está entre una curva y el eje de giro, así que cada
 * rebanada es un disco de radio R(x). {@code V = π∫[a, b] R(x)² dx}.
 */
public class DiskMethod implements VolumeMethod {

    @Override
    public String getName() {
        return "Discos";
    }

    @Override
    public String getFormula() {
        return "V = π∫[a, b] R(x)² dx";
    }

    @Override
    public Expression getFactor() {
        return Constant.PI;
    }

    @Override
    public Slice slice(Curve upper, Curve lower, RotationAxis axis, double middle) {
        // La curva que no está sobre el eje es la que da el radio
        Curve curve = Radius.isOnAxis(lower, axis) ? upper : lower;
        Radius radius = Radius.of(curve, axis, middle);
        Expression integrand = Simplifier.expand(new Power(radius.value(), new Constant(2)));
        return new Slice(integrand, "radio R(x) = " + radius.text());
    }
}
