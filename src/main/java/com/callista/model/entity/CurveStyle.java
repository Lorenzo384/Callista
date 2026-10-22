package com.callista.model.entity;

/**
 * Papel de una curva en la gráfica. La vista elige el color y el trazo
 * según el papel, para que las curvas se distingan aun sin color.
 */
public enum CurveStyle {

    /** La función que escribió el usuario: trazo continuo. */
    FUNCTION,
    /** Una derivada de la función: trazo discontinuo. */
    DERIVATIVE,
    /** Una antiderivada F(x) de la función: trazo discontinuo. */
    ANTIDERIVATIVE,
    /** Una recta auxiliar, como la tangente: trazo punteado. */
    TANGENT,
    /** La segunda función, g(x), en el área entre curvas: trazo discontinuo. */
    SECOND_FUNCTION
}
