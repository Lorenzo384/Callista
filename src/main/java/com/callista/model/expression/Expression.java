package com.callista.model.expression;

/**
 * Nodo de un árbol de expresión matemática de una variable.
 *
 * <p>Toda fórmula se representa como un árbol de objetos (patrón Composite):
 * las hojas son números ({@link Constant}) y variables ({@link Variable}),
 * y los nodos internos son operaciones que contienen otras expresiones.
 * Por ejemplo, {@code 3*x^2 + 1} es una {@link Sum} cuyo hijo izquierdo es un
 * {@link Product} y cuyo hijo derecho es la constante 1.</p>
 *
 * <p>Las implementaciones son inmutables.</p>
 */
public interface Expression {

    /** Precedencia de la suma y la resta. */
    int ADDITIVE = 1;
    /** Precedencia de la multiplicación y la división. */
    int MULTIPLICATIVE = 2;
    /** Precedencia del signo negativo. */
    int UNARY = 3;
    /** Precedencia de la potencia. */
    int POWER = 4;
    /** Precedencia de números y variables: nunca necesitan paréntesis. */
    int ATOM = 5;

    /**
     * Calcula el valor numérico de la expresión.
     *
     * <p>Las operaciones indefinidas no lanzan excepción: siguen las reglas de
     * {@code double} y devuelven {@code Infinity} o {@code NaN}
     * (por ejemplo, 1/0 o la raíz de un número negativo).</p>
     *
     * @param x valor que toma la variable
     * @return el resultado de evaluar la expresión en {@code x}
     */
    double evaluate(double x);

    /**
     * Prioridad del operador de este nodo; un valor mayor se agrupa primero.
     * Se usa para decidir dónde hacen falta paréntesis al convertir a texto.
     *
     * @return una de las constantes {@link #ADDITIVE}, {@link #MULTIPLICATIVE},
     *         {@link #UNARY}, {@link #POWER} o {@link #ATOM}
     */
    int precedence();

    /**
     * Representación en texto plano, con los paréntesis mínimos necesarios,
     * por ejemplo {@code 3*x^2 + 1}.
     */
    @Override
    String toString();
}
