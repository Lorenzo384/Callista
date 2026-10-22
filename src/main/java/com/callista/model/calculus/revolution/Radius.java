package com.callista.model.calculus.revolution;

import com.callista.model.calculus.revolution.VolumeMethod.Curve;
import com.callista.model.expression.Constant;
import com.callista.model.expression.Difference;
import com.callista.model.expression.Expression;
import com.callista.model.expression.Simplifier;

/**
 * Distancia de una curva al eje, sin valor absoluto: {@code f(x) - k} si la
 * curva está por encima (o a la derecha) del eje en el tramo, o
 * {@code k - f(x)} si está por debajo.
 *
 * @param value la distancia como expresión
 * @param text  cómo se escribe en el procedimiento: {@code 2 - f(x) = 2 - x^2}
 */
record Radius(Expression value, String text) {

    static Radius of(Curve curve, RotationAxis axis, double middle) {
        double k = axis.value();
        double at = curve.expression().evaluate(middle);
        if (k == 0) {
            boolean positive = at >= 0;
            Expression value = positive ? curve.expression()
                    : Simplifier.simplify(new Difference(new Constant(0), curve.expression()));
            String name = positive ? curve.name() : "-" + curve.name();
            return new Radius(value, name.equals(value.toString()) ? name : name + " = " + value);
        }
        boolean above = at >= k;
        Expression value = Simplifier.simplify(above
                ? new Difference(curve.expression(), axis.k())
                : new Difference(axis.k(), curve.expression()));
        String name = above ? curve.name() + " - " + axis.k() : axis.k() + " - " + curve.name();
        return new Radius(value, name + " = " + value);
    }

    /** Si la curva es la propia recta del eje (como g = 0 con el eje x). */
    static boolean isOnAxis(Curve curve, RotationAxis axis) {
        Expression distance = Simplifier.simplify(new Difference(curve.expression(), axis.k()));
        return distance instanceof Constant c && c.isNumber(0);
    }
}
