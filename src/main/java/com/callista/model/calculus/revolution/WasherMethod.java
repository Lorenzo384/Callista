package com.callista.model.calculus.revolution;

import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Power;
import com.callista.model.expression.Simplifier;

/**
 * Arandelas: la región está entre dos curvas, separada del eje, así que cada
 * rebanada es un disco de radio exterior R(x) con un hueco de radio interior
 * r(x). {@code V = π∫[a, b] (R(x)² - r(x)²) dx}.
 *
 * <p>Si en un tramo la región cruza el eje, la rebanada no tiene hueco: es un
 * disco con el radio mayor (la parte del otro lado queda adentro).</p>
 */
public class WasherMethod implements VolumeMethod {

    @Override
    public String getName() {
        return "Arandelas";
    }

    @Override
    public String getFormula() {
        return "V = π∫[a, b] (R(x)² - r(x)²) dx";
    }

    @Override
    public Expression getFactor() {
        return Constant.PI;
    }

    @Override
    public Slice slice(Curve upper, Curve lower, RotationAxis axis, double middle) {
        double k = axis.value();
        double up = upper.expression().evaluate(middle) - k;
        double down = lower.expression().evaluate(middle) - k;
        Curve far = Math.abs(up) >= Math.abs(down) ? upper : lower;
        Curve near = far == upper ? lower : upper;
        Radius outer = Radius.of(far, axis, middle);
        Expression outerSquared = new Power(outer.value(), new Constant(2));
        if (Math.signum(up) * Math.signum(down) < 0) {
            Expression integrand = Simplifier.expand(outerSquared);
            return new Slice(integrand, "la región cruza el eje " + axis.equation()
                    + ": cada rebanada es un disco lleno, R(x) = " + outer.text() + ", r(x) = 0");
        }
        Radius inner = Radius.of(near, axis, middle);
        Expression integrand = Simplifier.expand(new Difference(outerSquared,
                new Power(inner.value(), new Constant(2))));
        return new Slice(integrand, "radio exterior R(x) = " + outer.text() + ", radio interior r(x) = "
                + inner.text());
    }
}
