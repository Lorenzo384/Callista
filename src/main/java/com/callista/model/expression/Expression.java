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
    /**
     * Evalúa una expresión usando valores independientes para x e y.
     *
     * La variable y se sustituye por una constante antes de evaluar
     * el resultado respecto de x.
     *
     * @param x valor de la variable x
     * @param y valor de la variable y
     * @return resultado numérico de la expresión
     */
    default double evaluate(double x, double y) {
        Expression expressionWithYReplaced =
                replace(new Variable("y"), new Constant(y));

        return expressionWithYReplaced.evaluate(x);
    }

    boolean dependsOnVariable();

    /**
     * Copia de la expresión donde cada aparición de {@code target} se cambia
     * por {@code replacement}. Se usa en la sustitución: cambiar {@code x^2}
     * por {@code u}, o regresar de {@code u} a {@code x^2}.
     */
    /**
     * Indica si la expresión contiene una variable específica.
     *
     * @param variableName nombre de la variable que se busca
     * @return true si la expresión contiene esa variable
     */
    default boolean dependsOn(String variableName) {
        return contains(new Variable(variableName));
    }

    Expression replace(Expression target, Expression replacement);

    /**
     * Indica si {@code part} aparece en el árbol (incluida la expresión entera).
     */
    boolean contains(Expression part);

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
