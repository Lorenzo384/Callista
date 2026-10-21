package com.callista.model.calculus.derivative;

/**
 * Reglas de derivación que aplica {@link DerivativeCalculator}. Cada una
 * tiene el nombre con que se enseña en clase y su fórmula general, que se
 * muestran en el procedimiento.
 */
public enum DerivativeRule {

    CONSTANT("Regla de la constante", "d/dx[c] = 0"),
    VARIABLE("Derivada de la variable", "d/dx[x] = 1"),
    SUM("Regla de la suma", "d/dx[f + g] = f' + g'"),
    DIFFERENCE("Regla de la diferencia", "d/dx[f - g] = f' - g'"),
    SUM_AND_DIFFERENCE("Regla de la suma y la diferencia", "d/dx[f ± g] = f' ± g'"),
    NEGATION("Regla del signo", "d/dx[-f] = -f'"),
    CONSTANT_MULTIPLE("Regla del múltiplo constante", "d/dx[c*f] = c*f'"),
    PRODUCT("Regla del producto", "d/dx[f*g] = f'*g + f*g'"),
    QUOTIENT("Regla del cociente", "d/dx[f/g] = (f'*g - f*g')/g^2"),
    POWER("Regla de la potencia", "d/dx[x^n] = n*x^(n - 1)"),
    EXPONENTIAL("Regla de la exponencial", "d/dx[a^x] = a^x*ln(a)"),
    LOGARITHMIC("Derivación logarítmica", "d/dx[f^g] = f^g*(g'*ln(f) + g*f'/f)"),
    ELEMENTARY_FUNCTION("Derivada de una función elemental", "tabla de derivadas"),
    CHAIN("Regla de la cadena", "d/dx[f(u)] = f'(u)*u'");

    private final String displayName;
    private final String formula;

    DerivativeRule(String displayName, String formula) {
        this.displayName = displayName;
        this.formula = formula;
    }

    /**
     * Nombre de la regla, por ejemplo «Regla del producto».
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Fórmula general, por ejemplo {@code d/dx[f*g] = f'*g + f*g'}.
     */
    public String getFormula() {
        return formula;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
