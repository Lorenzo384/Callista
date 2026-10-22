package com.callista.model.calculus.revolution;

import com.callista.model.expression.Expression;

/**
 * Método para plantear el volumen de un sólido de revolución (patrón
 * <i>Strategy</i>): discos, arandelas o capas cilíndricas.
 *
 * <p>Cada método da, para un tramo de la región, el integrando sin el factor
 * constante (π o 2π) y la explicación de los radios o de la capa.</p>
 */
public interface VolumeMethod {

    /** «Discos», «Arandelas», «Capas cilíndricas». */
    String getName();

    /** La fórmula general, por ejemplo {@code V = π∫[a, b] R(x)² dx}. */
    String getFormula();

    /** El factor constante delante de la integral: π o 2π. */
    Expression getFactor();

    /**
     * Plantea un tramo.
     *
     * @param upper  la curva de arriba en el tramo
     * @param lower  la curva de abajo
     * @param axis   el eje de giro
     * @param middle un punto dentro del tramo, para decidir signos
     */
    Slice slice(Curve upper, Curve lower, RotationAxis axis, double middle);

    /**
     * Una curva con su nombre en el procedimiento: {@code f(x)} o {@code g(x)}.
     */
    record Curve(String name, Expression expression) {
    }

    /**
     * Lo que se integra en un tramo.
     *
     * @param integrand   el integrando, sin el factor constante
     * @param description cómo se obtuvo: radios, o radio y altura de la capa
     */
    record Slice(Expression integrand, String description) {
    }
}
