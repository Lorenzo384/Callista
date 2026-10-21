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
     * Indica si la expresión depende de la variable. Las que no dependen
     * (como {@code 3}, {@code π^2} o {@code ln(2)}) se tratan como constantes
     * al derivar o integrar.
     */
    boolean dependsOnVariable();

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

    /**
     * Igualdad <b>estructural</b>: dos expresiones son iguales si tienen el
     * mismo árbol ({@code x^2} y {@code x^2}). No reconoce equivalencias
     * matemáticas ({@code x + x} no es igual a {@code 2*x}); para eso se
     * simplifican primero.
     */
    @Override
    boolean equals(Object other);

    @Override
    int hashCode();
}
