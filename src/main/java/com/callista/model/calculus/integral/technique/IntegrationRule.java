package com.callista.model.calculus.integral.technique;

/**
 * Reglas y técnicas de integración que se explican en el procedimiento, con
 * el nombre con que se enseñan en clase y su fórmula general.
 */
public enum IntegrationRule {

    CONSTANT("Integral de una constante", "∫c dx = c·x"),
    LINEARITY("Linealidad", "∫(f ± g) dx = ∫f dx ± ∫g dx"),
    CONSTANT_MULTIPLE("Múltiplo constante", "∫c·f dx = c·∫f dx"),
    POWER("Regla de la potencia", "∫xⁿ dx = xⁿ⁺¹/(n + 1), n ≠ -1"),
    LOGARITHM("Integral de 1/x", "∫1/x dx = ln|x|"),
    TABLE("Integral inmediata", "tabla de integrales"),
    SUBSTITUTION("Sustitución", "∫f(g(x))·g'(x) dx = ∫f(u) du, con u = g(x)"),
    BACK_SUBSTITUTION("Regresar a la variable original", "u = g(x)"),
    EXPANSION("Desarrollar el producto", "multiplicar para obtener una suma de términos"),
    BY_PARTS("Integración por partes", "∫u dv = u·v - ∫v du"),
    CYCLIC_INTEGRAL("La integral original vuelve a aparecer", "I = A + m·I  ⇒  I = A/(1 - m)");

    private final String displayName;
    private final String formula;

    IntegrationRule(String displayName, String formula) {
        this.displayName = displayName;
        this.formula = formula;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormula() {
        return formula;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
